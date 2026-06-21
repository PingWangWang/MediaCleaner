package com.photocleaner.feature.duplicate.domain.usecase

import android.graphics.BitmapFactory
import com.photocleaner.core.common.model.DuplicateGroup
import com.photocleaner.core.common.model.GroupType
import com.photocleaner.core.common.model.ImageItem
import com.photocleaner.feature.duplicate.graph.SimilarityGraph
import com.photocleaner.feature.duplicate.hash.DHashCalculator
import com.photocleaner.feature.duplicate.hash.PHashCalculator
import com.photocleaner.feature.duplicate.lsh.LshClusterAlgorithm
import com.photocleaner.feature.duplicate.lsh.LshClusterResult
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
        private const val MEDIUM_SIMILARITY_THRESHOLD = 25

        /** 用于 Union-Find 建边的相似度阈值（64 位中的最大差异位） */
        private const val SIMILARITY_GRAPH_THRESHOLD = 15

        /**
         * pHash 门控灰色地带（设计文档要求：dHash 距离 5~8 的边界组）
         * 在此范围内的组需要 pHash 进一步验证
         */
        private const val PHASH_GRAY_ZONE_LOWER = 5
        private const val PHASH_GRAY_ZONE_UPPER = 8

        /**
         * ORB 三门控条件（设计文档）：
         * 条件一：组内图片数量 > 8
         * 条件二：dHash 距离在 5~8 的灰色地带
         * 条件三：pHash 距离在 10~20 的灰色地带
         */
        private const val ORB_MAX_GROUP_SIZE = 8
        private const val ORB_DHASH_GRAY_LOWER = 5
        private const val ORB_DHASH_GRAY_UPPER = 8
        private const val ORB_PHASH_GRAY_LOWER = 10
        private const val ORB_PHASH_GRAY_UPPER = 20

        /** ORB 匹配相似度阈值（NCC 映射后 >= 此值视为相似） */
        private const val ORB_SIMILARITY_THRESHOLD = 0.8f

        /** 图片加载的采样尺寸上限（避免 OOM） */
        private const val LOAD_SAMPLE_SIZE = 512

        /**
         * 保留权重量化评分体系（设计文档 Section 5.8）
         * 满分 100 分
         */
        private const val WEIGHT_RESOLUTION = 35  // 分辨率权重
        private const val WEIGHT_SIZE = 15         // 文件大小权重
        private const val WEIGHT_FORMAT = 10       // 图片格式权重
        private const val WEIGHT_SOURCE = 25       // 来源路径权重
        private const val WEIGHT_EXIF = 10         // EXIF 信息权重
        private const val WEIGHT_TIME = 5          // 修改时间权重
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
            // Layer 2b: LSH 聚类（O(n)）+ 置信度门控
            // ──────────────────────────────────────────────────────
            val clusters = lshClusterAlgorithm.cluster(
                images = validImages,
                hashFunc = { it.dHash ?: "" }
            )

            for (clusterResult in clusters) {
                val cluster = clusterResult.images
                if (cluster.size < 2) continue

                // 基于 LSH 置信度分流的门控决策：
                // confidence >= 0.85 → 直接判定为重复，跳过 dHash 两两比较
                // confidence 0.6~0.85 → 需要 dHash Union-Find 进一步验证
                val isDirectMatch = clusterResult.confidence >= 0.85f

                val groups = if (isDirectMatch) {
                    // 高置信度：整个 cluster 作为一个组
                    listOf(cluster)
                } else {
                    // ──────────────────────────────────────────────
                    // Layer 3: Union-Find 相似图防链式误报
                    // ──────────────────────────────────────────────
                    buildSimilarityGroups(cluster)
                }

                for (group in groups) {
                    if (group.size < 2) continue

                    // ──────────────────────────────────────────────
                    // Layer 4: pHash 边界检查（dHash 距离 5~8 的灰色地带）
                    // ──────────────────────────────────────────────
                    val initialGroupType = determineGroupType(group)
                    val needsPHash = initialGroupType == GroupType.MEDIUM_SIMILARITY &&
                        hasDHashInGrayZone(group, PHASH_GRAY_ZONE_LOWER, PHASH_GRAY_ZONE_UPPER)
                    val refinedGroup = if (needsPHash) {
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
                    // 加权评分选择最优图片
                    // ──────────────────────────────────────────────
                    val sortedGroup = finalGroup.sortedByDescending { computeWeightedScore(it) }
                    val bestImage = sortedGroup.first()
                    val canDeleteImages = sortedGroup.drop(1)
                    val totalSize = canDeleteImages.sumOf { it.size }

                    emit(
                        DuplicateGroup(
                            groupId = groupIdCounter.incrementAndGet(),
                            images = sortedGroup,
                            similarity = computeGroupSimilarity(finalGroup),
                            groupType = determineGroupType(finalGroup),
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
     * 判断组内是否有任意两图片的 dHash 距离在指定灰色地带范围内。
     */
    private fun hasDHashInGrayZone(group: List<ImageItem>, lower: Int, upper: Int): Boolean {
        for (i in group.indices) {
            for (j in i + 1 until group.size) {
                val hash1 = group[i].dHash ?: continue
                val hash2 = group[j].dHash ?: continue
                val distance = hammingDistanceMatcher.computeHammingDistance(hash1, hash2)
                if (distance in lower..upper) return true
            }
        }
        return false
    }

    /**
     * 保留权重量化评分体系（设计文档 Section 5.8）。
     *
     * 满分 100 分，综合评估图片质量：
     * - 分辨率 35 分：像素越高分越高（上限 5000 万像素）
     * - 文件大小 15 分：越大越好（上限 20MB）
     * - 图片格式 10 分：无损格式优先（PNG=10, WebP=7, JPEG=5, GIF=2, 其他=0）
     * - 来源路径 25 分：相机/DCMI 路径优先（包含 DCIM/Camera=25, Pictures=15, Download=5, 其他=0）
     * - EXIF 10 分：含 EXIF 信息为 10 分，否则 0
     * - 修改时间 5 分：越新越高（近 7 天=5, 近 30 天=3, 近 90 天=1, 更早=0）
     */
    private fun computeWeightedScore(image: ImageItem): Int {
        var score = 0

        // 1. 分辨率评分 (0~35)
        val megapixels = ((image.width ?: 0).toLong() * (image.height ?: 0)) / 1_000_000f
        score += when {
            megapixels >= 50 -> 35
            megapixels >= 20 -> 30
            megapixels >= 10 -> 25
            megapixels >= 5 -> 20
            megapixels >= 2 -> 15
            megapixels >= 1 -> 10
            else -> 5
        }

        // 2. 文件大小评分 (0~15)，上限 20MB
        val sizeMB = image.size / (1024f * 1024f)
        score += when {
            sizeMB >= 20 -> 15
            sizeMB >= 10 -> 12
            sizeMB >= 5 -> 10
            sizeMB >= 2 -> 7
            sizeMB >= 1 -> 5
            else -> 3
        }

        // 3. 图片格式评分 (0~10)
        score += when {
            image.mimeType?.contains("png") == true -> 10
            image.mimeType?.contains("webp") == true -> 7
            image.mimeType?.contains("jpeg") == true || image.mimeType?.contains("jpg") == true -> 5
            image.mimeType?.contains("gif") == true -> 2
            else -> 0
        }

        // 4. 来源路径评分 (0~25) - 检查路径中包含的关键词
        val path = image.path ?: image.uri
        score += when {
            path.contains("DCIM", ignoreCase = true) || path.contains("Camera", ignoreCase = true) -> 25
            path.contains("Pictures", ignoreCase = true) || path.contains("Screenshots", ignoreCase = true) -> 15
            path.contains("Download", ignoreCase = true) || path.contains("WeiXin", ignoreCase = true) -> 5
            else -> 0
        }

        // 5. EXIF 评分 (0~10) - 简化：有 orientation 或宽高信息推断有 EXIF
        val hasExif = image.orientation != 0 || (image.width != null && image.height != null)
        if (hasExif) score += 10

        // 6. 修改时间评分 (0~5)
        val ageDays = (System.currentTimeMillis() - image.modifyTime) / (24 * 60 * 60 * 1000)
        score += when {
            ageDays <= 7 -> 5
            ageDays <= 30 -> 3
            ageDays <= 90 -> 1
            else -> 0
        }

        return score
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
     * 三门控条件（设计文档 Section 5.9）：
     * 条件一：组内图片数量 > 8
     * 条件二：存在任意图片对 dHash 距离在 5~8 的灰色地带
     * 条件三：存在任意图片对 pHash 距离在 10~20 的灰色地带
     */
    private fun shouldEnableOrbCheckForGroup(group: List<ImageItem>): Boolean {
        if (group.size < 2) return false

        // 条件一：组内图片数量 > 8
        if (group.size <= ORB_MAX_GROUP_SIZE) return false

        // 条件二：dHash 在灰色地带
        val hasDHashGray = hasDHashInGrayZone(group, ORB_DHASH_GRAY_LOWER, ORB_DHASH_GRAY_UPPER)
        if (!hasDHashGray) return false

        // 条件三：pHash 在灰色地带（需要先计算 pHash）
        // 为缺失 pHash 的图片计算
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
        val validPHash = imagesWithPHash.filter { it.pHash != null && it.pHash!!.length == 64 }
        if (validPHash.size < 2) return false

        // 检查 pHash 距离是否在 10~20 范围
        for (i in validPHash.indices) {
            for (j in i + 1 until validPHash.size) {
                val hash1 = validPHash[i].pHash ?: continue
                val hash2 = validPHash[j].pHash ?: continue
                val distance = hammingDistanceMatcher.computeHammingDistance(hash1, hash2)
                if (distance in ORB_PHASH_GRAY_LOWER..ORB_PHASH_GRAY_UPPER) {
                    return true
                }
            }
        }

        return false
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
