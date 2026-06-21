package com.photocleaner.feature.duplicate.lsh

import com.photocleaner.core.common.model.ImageItem

/**
 * LSH 聚类结果中的数据项。
 *
 * @property image    图片项
 * @property hash     该图片使用的哈希值
 */
data class LshCluster(
    val images: List<ImageItem>
)

/**
 * LSH 聚类算法。
 *
 * 将 64 位哈希拆分为 [numBands] 个 band，每个 band 包含 [numRows] 位。
 * 对于每个 band，将其位子串作为键存入哈希桶；同一桶中的图片视为候选相似对。
 * 因每个 band 独立建桶，只要任意一个 band 碰撞即被召回，时间复杂度 O(n)。
 *
 * @author PhotoCleaner
 */
object LshClusterAlgorithm {

    /**
     * 对图片集合执行 LSH 聚类。
     *
     * @param images   待聚类的图片列表
     * @param hashFunc 从 [ImageItem] 提取 64 位二进制哈希字符串的函数
     * @param numBands 拆分的 band 数量（默认 8）
     * @param numRows  每个 band 的比特位数（默认 8），要求 numBands * numRows == 64
     * @return 候选重复图片组的列表
     */
    fun cluster(
        images: List<ImageItem>,
        hashFunc: (ImageItem) -> String,
        numBands: Int = 8,
        numRows: Int = 8
    ): List<List<ImageItem>> {
        require(numBands * numRows == 64) {
            "numBands * numRows must equal 64, but got $numBands * $numRows = ${numBands * numRows}"
        }

        if (images.size < 2) return emptyList()

        // 为每个 band 维护一个哈希桶：bandIndex -> (bandValue -> images)
        val buckets = Array<MutableMap<String, MutableList<ImageItem>>>(numBands) {
            mutableMapOf()
        }

        for (image in images) {
            val hash = hashFunc(image)
            if (hash.length != 64) continue

            for (band in 0 until numBands) {
                val start = band * numRows
                val end = start + numRows
                // 确保不越界
                if (end > hash.length) continue
                val bandValue = hash.substring(start, end)

                buckets[band]
                    .getOrPut(bandValue) { mutableListOf() }
                    .add(image)
            }
        }

        // 收集所有有碰撞的桶（>=2 张图片）作为候选组
        // 使用 Set 按 ImageItem.id 去重，避免同一张图片在多个 band 中被重复计入
        val candidates = mutableSetOf<Set<ImageItem>>()

        for (band in 0 until numBands) {
            for ((_, group) in buckets[band]) {
                if (group.size >= 2) {
                    candidates.add(group.toSet())
                }
            }
        }

        // 过滤：如果有两个候选组互为子集，只保留较大的组
        val sorted = candidates.sortedByDescending { it.size }
        val result = mutableListOf<List<ImageItem>>()
        val seen = mutableSetOf<Long>()

        for (group in sorted) {
            val ids = group.map { it.id }.toSet()
            if (ids.any { it in seen }) {
                // 该组中的某些图片已被归入更大的组，跳过
                continue
            }
            seen.addAll(ids)
            result.add(group.toList())
        }

        return result
    }
}
