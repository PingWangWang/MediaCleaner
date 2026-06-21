package com.photocleaner.feature.duplicate.domain.usecase

import android.graphics.BitmapFactory
import com.photocleaner.core.common.model.DuplicateGroup
import com.photocleaner.core.common.model.GroupType
import com.photocleaner.core.common.model.ImageItem
import com.photocleaner.feature.duplicate.graph.SimilarityGraph
import com.photocleaner.feature.duplicate.hash.DHashCalculator
import com.photocleaner.feature.duplicate.hash.PHashCalculator
import com.photocleaner.feature.duplicate.lsh.LshClusterAlgorithm
import com.photocleaner.feature.duplicate.matcher.HammingDistanceMatcher
import com.photocleaner.feature.duplicate.matcher.OrbFeatureMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 重复图片检测用例。
 *
 * 实现五层流水线检测策略：
 * - Layer 1：按 sizeBucket + ratioBucket 预分组（SQL 层面已处理）
 * - Layer 2：dHash + LSH 快速聚类（O(n)）
 * - Layer 3：Union-Find 相似图防链式误报
 * - Layer 4：pHash 边界检查（灰色地带图片）
 * - Layer 5：ORB 精细化匹配（三门控条件）
 *
 * 以 [Flow] 形式逐个发射 [DuplicateGroup]，支持流式消费。
 *
 * @author PhotoCleaner
 */
@Singleton
class DetectDuplicateUseCase @Inject constructor(
    private val dHashCalculator: DHashCalculator,
    private val pHashCalculator: PHashCalculator,
    private val hammingDistanceMatcher: HammingDistanceMatcher,
    private val lshClusterAlgorithm: LshClusterAlgorithm,
    private val similarityGraph: SimilarityGraph,
    private val orbFeatureMatcher: OrbFeatureMatcher
) {

    companion object {
        /** 精确重复的汉明距离阈值（距离 == 0 才是精确重复） */
        private const val EXACT_MATCH_THRESHOLD = 0

        /** 高相似度阈值（汉明距离 <= 此值视为高相似） */
        private const val HIGH_SIMILARITY_THRESHOLD = 10

        /** 中等相似度阈值 */
        private const val MEDIUM_SIMILARITY_THRESHOLD = 20

        /** 用于 Union-Find 建边的相似度阈值（64 位中的最大差异位） */
        private const val SIMILARITY_GRAPH_THRESHOLD = 15

        /** 灰色地带下限：汉明距离 >= 此值时触发 ORB 检查 */
        private const val ORB_GRAY_ZONE_LOWER = 5

        /** 灰色地带上限：汉明距离 <= 此值时触发 ORB 检查 */
        private const val ORB_GRAY_ZONE_UPPER = 15

        /** ORB 检查门控条件二：组内图片数量上限 */
        private const val ORB_MAX_GROUP_SIZE = 10

        /** ORB 检查门控条件三：图片尺寸差异比例上限 */
        private const val ORB_SIZE_RATIO_TOLERANCE = 0.3f

        /** ORB 匹配相似度阈值（NCC 映射后 >= 此值视为相似） */
        private const val ORB_SIMILARITY_THRESHOLD = 0.8f

        /** 图片加载的采样尺寸上限（避免 OOM） */
        private const val LOAD_SAMPLE_SIZE = 512
    }

    /** 自增 GroupId 生成器 */
    private val groupIdCounter = AtomicLong(0)

    /**
     * 执行重复检测。
     *
     * @param images 待检测的图片列表
     * @return 发射 [DuplicateGroup] 的流
     */
    operator fun invoke(images: List<ImageItem>): Flow<DuplicateGroup> = flow {
        if (images.size < 2) return@flow

        // ──────────────────────────────────────────────────────────
        // Layer 1: 按 sizeBucket + ratioBucket 预分组
        // ──────────────────────────────────────────────────────────
        val buckets = images.groupBy { it.sizeBucket to it.ratioBucket }

        for ((_, bucketImages) in buckets) {
            if (bucketImages.size < 2) continue

            // ──────────────────────────────────────────────────────
            // Layer 2a: 计算 dHash（若尚未计算）
            // ──────────────────────────────────────────────────────
            val imagesWithHash = bucketImages.map { image ->
                if (image.dHash != null) {
                    image
                } else {
                    val bitmap = loadBitmap(image.path)
                    if (bitmap != null) {
                        val hash = dHashCalculator.calculateHash(bitmap)
                        image.copy(dHash = hash, isCalculated = true)
                    } else {
                        image
                    }
                }
            }

            val validImages = imagesWithHash.filter { it.dHash != null && it.dHash!!.length == 64 }
            if (validImages.size < 2) continue

            // ──────────────────────────────────────────────────────
            // Layer 2b: LSH 聚类（O(n)）
            // ──────────────────────────────────────────────────────
            val clusters = lshClusterAlgorithm.cluster(
                images = validImages,
                hashFunc = { it.dHash ?: "" }
            )

            for (cluster in clusters) {
                if (cluster.size < 2) continue

                // ──────────────────────────────────────────────────
                // Layer 3: Union-Find 相似图防链式误报
                // ──────────────────────────────────────────────────
                val groups = buildSimilarityGroups(cluster)
                for (group in groups) {
                    if (group.size < 2) continue

                    // ──────────────────────────────────────────────
                    // Layer 4: pHash 边界检查（灰色地带图片重评估）
                    // ──────────────────────────────────────────────
                    val initialGroupType = determineGroupType(group)
                    val refinedGroup = if (initialGroupType == GroupType.MEDIUM_SIMILARITY) {
                        refineWithPHash(group)
                    } else {
                        group
                    }
                    if (refinedGroup.size < 2) continue

                    // ──────────────────────────────────────────────
                    // Layer 5: ORB 精细化匹配（三条件门控）
                    // ──────────────────────────────────────────────
                    val finalGroup = if (shouldEnableOrbCheckForGroup(refinedGroup)) {
                        refineWithOrb(refinedGroup)
                    } else {
                        refinedGroup
                    }
                    if (finalGroup.size < 2) continue

                    // ──────────────────────────────────────────────
                    // 确定组类型并排序
                    // ──────────────────────────────────────────────
                    val groupType = determineGroupType(finalGroup)
                    val sortedGroup = finalGroup.sortedByDescending {
                        (it.width ?: 0) * (it.height ?: 0)
                    }
                    val bestImage = sortedGroup.first()
                    val canDeleteImages = sortedGroup.drop(1)
                    val totalSize = canDeleteImages.sumOf { it.size }

                    emit(
                        DuplicateGroup(
                            groupId = groupIdCounter.incrementAndGet(),
                            images = sortedGroup,
                            similarity = computeGroupSimilarity(finalGroup),
                            groupType = groupType,
                            bestImage = bestImage,
                            canDeleteImages = canDeleteImages,
                            size = totalSize
                        )
                    )
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 基于 dHash 汉明距离与 Union-Find 构建相似组，避免链式传播。
     */
    private fun buildSimilarityGroups(cluster: List<ImageItem>): List<List<ImageItem>> {
        val ids = cluster.map { it.id }
        similarityGraph.initialize(ids)

        // 两两比较汉明距离，将相似图片用边连接
        for (i in cluster.indices) {
            for (j in i + 1 until cluster.size) {
                val hash1 = cluster[i].dHash ?: continue
                val hash2 = cluster[j].dHash ?: continue
                val distance = hammingDistanceMatcher.computeHammingDistance(hash1, hash2)

                // 将汉明距离转换为相似度分数 [0, 1]
                val similarity = (64 - distance).coerceAtLeast(0).toFloat() / 64f
                val threshold = (64 - SIMILARITY_GRAPH_THRESHOLD).toFloat() / 64f

                similarityGraph.addEdge(cluster[i].id, cluster[j].id, similarity, threshold)
            }
        }

        // 获取连通分量
        val components = similarityGraph.getConnectedComponents()

        // 将每个分量中的 ID 映射回 ImageItem
        val idToItem = cluster.associateBy { it.id }
        return components.values.map { ids ->
            ids.mapNotNull { idToItem[id] }
        }
    }

    /**
     * 确定图片组的重复类型。
     *
     * 遍历组内所有两两组合，取最小距离作为分组判定依据。
     */
    private fun determineGroupType(group: List<ImageItem>): GroupType {
        var minDistance = Int.MAX_VALUE

        for (i in group.indices) {
            for (j in i + 1 until group.size) {
                val hash1 = group[i].dHash ?: continue
                val hash2 = group[j].dHash ?: continue
                val distance = hammingDistanceMatcher.computeHammingDistance(hash1, hash2)
                if (distance < minDistance) {
                    minDistance = distance
                }
            }
        }

        return when {
            minDistance <= EXACT_MATCH_THRESHOLD -> GroupType.EXACT_DUPLICATE
            minDistance <= HIGH_SIMILARITY_THRESHOLD -> GroupType.HIGH_SIMILARITY
            else -> GroupType.MEDIUM_SIMILARITY
        }
    }

    /**
     * 计算组内平均相似度。
     */
    private fun computeGroupSimilarity(group: List<ImageItem>): Float {
        if (group.size < 2) return 1f
        var totalSimilarity = 0f
        var count = 0

        for (i in group.indices) {
            for (j in i + 1 until group.size) {
                val hash1 = group[i].dHash ?: continue
                val hash2 = group[j].dHash ?: continue
                val distance = hammingDistanceMatcher.computeHammingDistance(hash1, hash2)
                totalSimilarity += (64 - distance).coerceAtLeast(0).toFloat() / 64f
                count++
            }
        }

        return if (count > 0) totalSimilarity / count else 1f
    }

    /**
     * 从文件路径加载位图（使用采样缩小以避免 OOM）。
     */
    private fun loadBitmap(path: String?): android.graphics.Bitmap? {
        if (path == null) return null
        return try {
            val opts = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, opts)

            val scaleFactor = maxOf(
                (opts.outWidth / LOAD_SAMPLE_SIZE).coerceAtLeast(1),
                (opts.outHeight / LOAD_SAMPLE_SIZE).coerceAtLeast(1)
            )

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = scaleFactor
            }
            BitmapFactory.decodeFile(path, decodeOpts)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 使用 pHash 对灰色地带图片组进行边界检查。
     *
     * 当 dHash 判定为中等相似时（汉明距离 11~20），计算 pHash 重新验证：
     * - 若 pHash 汉明距离 <= 10，提升为高度相似
     * - 否则保持中等相似分类
     */
    private fun refineWithPHash(group: List<ImageItem>): List<ImageItem> {
        // 为缺少 pHash 的图片计算 pHash
        val imagesWithPHash = group.map { image ->
            if (image.pHash != null) {
                image
            } else {
                val bitmap = loadBitmap(image.path)
                if (bitmap != null) {
                    val hash = pHashCalculator.calculateHash(bitmap)
                    image.copy(pHash = hash)
                } else {
                    image
                }
            }
        }

        val validImages = imagesWithPHash.filter { it.pHash != null && it.pHash!!.length == 64 }
        if (validImages.size < 2) return group

        // 使用 pHash 重新计算两两之间的汉明距离
        // 若任意两张图片的 pHash 距离 <= 10，将它们保留在同一组
        val keptIds = mutableSetOf<Long>()
        for (i in validImages.indices) {
            for (j in i + 1 until validImages.size) {
                val hash1 = validImages[i].pHash ?: continue
                val hash2 = validImages[j].pHash ?: continue
                val distance = hammingDistanceMatcher.computeHammingDistance(hash1, hash2)
                if (distance <= HIGH_SIMILARITY_THRESHOLD) {
                    keptIds.add(validImages[i].id)
                    keptIds.add(validImages[j].id)
                }
            }
        }

        // 至少引入一张图片才进行过滤
        if (keptIds.size < 2) return group

        return group.filter { it.id in keptIds }
    }

    /**
     * 判断整组图片是否应启用 ORB 精细化匹配。
     *
     * 检查组内所有两两距离，若至少有任意一对处于灰色地带，
     * 且同时满足图片数量与尺寸比例条件，则启用 ORB 检查。
     */
    private fun shouldEnableOrbCheckForGroup(group: List<ImageItem>): Boolean {
        if (group.size < 2 || group.size > ORB_MAX_GROUP_SIZE) return false

        // 检查是否至少有一对在灰色地带
        var hasGrayZone = false
        for (i in group.indices) {
            for (j in i + 1 until group.size) {
                val hash1 = group[i].dHash ?: continue
                val hash2 = group[j].dHash ?: continue
                val distance = hammingDistanceMatcher.computeHammingDistance(hash1, hash2)
                if (distance in ORB_GRAY_ZONE_LOWER..ORB_GRAY_ZONE_UPPER) {
                    hasGrayZone = true
                    break
                }
            }
            if (hasGrayZone) break
        }
        if (!hasGrayZone) return false

        // 条件三：尺寸比例检查
        return shouldEnableOrbCheck(0, group.size, group)
    }

    /**
     * 判断是否应启用 ORB 精细化匹配的三个门控条件。
     *
     * 条件一：dHash 汉明距离处于灰色地带 [ORB_GRAY_ZONE_LOWER, ORB_GRAY_ZONE_UPPER]
     * 条件二：组内图片数量不超过 [ORB_MAX_GROUP_SIZE]
     * 条件三：图片之间尺寸差异不超过 [ORB_SIZE_RATIO_TOLERANCE]
     */
    private fun shouldEnableOrbCheck(
        distance: Int,
        groupSize: Int,
        images: List<ImageItem>
    ): Boolean {
        // 条件一：灰色地带
        if (distance < ORB_GRAY_ZONE_LOWER || distance > ORB_GRAY_ZONE_UPPER) return false

        // 条件二：组大小上限
        if (groupSize > ORB_MAX_GROUP_SIZE) return false

        // 条件三：尺寸比例检查（取组内最大/最小面积比）
        val areas = images.mapNotNull { img ->
            val w = img.width ?: return@mapNotNull null
            val h = img.height ?: return@mapNotNull null
            w * h
        }
        if (areas.size < 2) return false

        val maxArea = areas.maxOrNull() ?: return false
        val minArea = areas.minOrNull() ?: return false
        if (minArea == 0) return false

        val ratio = maxArea.toFloat() / minArea.toFloat()
        return ratio <= (1f + ORB_SIZE_RATIO_TOLERANCE)
    }

    /**
     * 对组内图片执行 ORB 精细化匹配（灰度地带细分）。
     *
     * 对于灰色地带的图片对，使用 [OrbFeatureMatcher] 进行更精确的相似度判定，
     * 将不满足 [ORB_SIMILARITY_THRESHOLD] 的图片从组中剔除。
     *
     * @return 经过 ORB 过滤后的图片组
     */
    private fun refineWithOrb(group: List<ImageItem>): List<ImageItem> {
        if (group.size < 2) return group

        // 加载所有图片的 Bitmap
        val bitmaps = group.mapNotNull { img ->
            val bmp = loadBitmap(img.path)
            if (bmp != null) img to bmp else null
        }
        if (bitmaps.size < 2) return group

        // 重新构建：仅保留通过 ORB 检查的图片
        val kept = mutableListOf(bitmaps.first().first)
        val keptBitmaps = mutableListOf(bitmaps.first().second)

        for (i in 1 until bitmaps.size) {
            val (img, bmp) = bitmaps[i]
            var isSimilar = false

            for (j in keptBitmaps.indices) {
                val score = orbFeatureMatcher.match(keptBitmaps[j], bmp)
                if (score >= ORB_SIMILARITY_THRESHOLD) {
                    isSimilar = true
                    break
                }
            }

            if (isSimilar) {
                kept.add(img)
                keptBitmaps.add(bmp)
            }
        }

        // 回收未使用的 Bitmap
        val keptIds = kept.map { it.id }.toSet()
        bitmaps.forEach { (img, bmp) ->
            if (img.id !in keptIds) {
                bmp.recycle()
            }
        }

        return kept
    }
}
