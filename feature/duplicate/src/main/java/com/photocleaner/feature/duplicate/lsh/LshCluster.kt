/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 局部敏感哈希(LSH)聚类算法
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.duplicate.lsh

import com.photocleaner.core.common.model.ImageItem

/**
 * LSH 聚类结果。
 *
 * @property images 候选组中的图片列表
 * @property confidence 置信度分数 (0.0~1.0)，碰撞段数/总段数
 */
data class LshClusterResult(
    val images: List<ImageItem>,
    val confidence: Float
)

/**
 * LSH 聚类算法。
 *
 * 将 64 位哈希拆分为 [numBands] 个 band，每个 band 包含 [numRows] 位。
 * 对于每个 band，将其位子串作为键存入哈希桶；同一桶中的图片视为候选相似对。
 *
 * **置信度门控机制**：
 * - confidence = 碰撞段数 / numBands（两张图片在多少个 band 中落在同一桶）
 * - confidence >= 0.85 → 直接判定为重复（无需进一步检查）
 * - confidence 0.6~0.85 → 需要 dHash 进一步验证
 * - confidence < 0.6 → 视为非重复，直接丢弃
 *
 * @author PhotoCleaner
 */
object LshClusterAlgorithm {

    /** 高置信度阈值：直接判定为重复 */
    private const val HIGH_CONFIDENCE_THRESHOLD = 0.85f

    /** 低置信度阈值：低于此值直接丢弃 */
    private const val LOW_CONFIDENCE_THRESHOLD = 0.6f

    /**
     * 对图片集合执行 LSH 聚类。
     *
     * @param images   待聚类的图片列表
     * @param hashFunc 从 [ImageItem] 提取 64 位二进制哈希字符串的函数
     * @param numBands 拆分的 band 数量（默认 8）
     * @param numRows  每个 band 的比特位数（默认 8），要求 numBands * numRows == 64
     * @return 候选重复图片组的列表（已通过置信度门控过滤）
     */
    fun cluster(
        images: List<ImageItem>,
        hashFunc: (ImageItem) -> String,
        numBands: Int = 8,
        numRows: Int = 8
    ): List<LshClusterResult> {
        require(numBands * numRows == 64) {
            "numBands * numRows must equal 64, but got $numBands * $numRows = ${numBands * numRows}"
        }

        if (images.size < 2) return emptyList()

        // 为每个 band 维护一个哈希桶：bandIndex -> (bandValue -> list of image)
        val buckets = Array<MutableMap<String, MutableList<ImageItem>>>(numBands) {
            mutableMapOf()
        }

        for (image in images) {
            val hash = hashFunc(image)
            if (hash.length != 64) continue

            for (band in 0 until numBands) {
                val start = band * numRows
                val end = start + numRows
                if (end > hash.length) continue
                val bandValue = hash.substring(start, end)

                buckets[band]
                    .getOrPut(bandValue) { mutableListOf() }
                    .add(image)
            }
        }

        // 计算每对图片的碰撞次数，用于置信度计算
        val collisionCounts = mutableMapOf<Pair<Long, Long>, Int>()

        for (band in 0 until numBands) {
            for ((_, group) in buckets[band]) {
                if (group.size < 2) continue
                for (i in group.indices) {
                    for (j in i + 1 until group.size) {
                        val key = if (group[i].id < group[j].id) {
                            group[i].id to group[j].id
                        } else {
                            group[j].id to group[i].id
                        }
                        collisionCounts[key] = (collisionCounts[key] ?: 0) + 1
                    }
                }
            }
        }

        // 根据碰撞次数构建相似图组
        val idToImages = images.associateBy { it.id }
        val visited = mutableSetOf<Long>()
        val adjacency = mutableMapOf<Long, MutableSet<Pair<Long, Float>>>()

        for ((pair, collisions) in collisionCounts) {
            val (id1, id2) = pair
            val confidence = collisions.toFloat() / numBands

            // 置信度门控：低于阈值直接丢弃
            if (confidence < LOW_CONFIDENCE_THRESHOLD) continue

            adjacency.getOrPut(id1) { mutableSetOf() }.add(id2 to confidence)
            adjacency.getOrPut(id2) { mutableSetOf() }.add(id1 to confidence)
        }

        // BFS 分组
        val results = mutableListOf<LshClusterResult>()

        for (image in images) {
            if (image.id in visited) continue

            val groupImages = mutableListOf<ImageItem>()
            val groupConfs = mutableListOf<Float>()
            val queue = ArrayDeque<Long>()
            queue.add(image.id)
            visited.add(image.id)

            while (queue.isNotEmpty()) {
                val currentId = queue.removeFirst()
                val currentImage = idToImages[currentId] ?: continue
                groupImages.add(currentImage)

                val neighbors = adjacency[currentId] ?: continue
                for ((neighborId, conf) in neighbors) {
                    if (neighborId !in visited) {
                        visited.add(neighborId)
                        queue.add(neighborId)
                        groupConfs.add(conf)
                    }
                }
            }

            if (groupImages.size >= 2) {
                val avgConf = if (groupConfs.isNotEmpty()) {
                    groupConfs.sum() / groupConfs.size
                } else {
                    1.0f
                }
                results.add(LshClusterResult(images = groupImages, confidence = avgConf))
            }
        }

        return results
    }
}
