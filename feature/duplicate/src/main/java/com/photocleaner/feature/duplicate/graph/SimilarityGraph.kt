package com.photocleaner.feature.duplicate.graph

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 相似度图。
 *
 * 使用并查集（Union-Find）将满足相似度阈值的图片对连接为连通分量。
 * 可有效避免链式虚假分组：A≈B 且 B≈C 但 A≉C 时，仍会将 A、B、C 放入同一组，
 * 调用方应通过 [threshold] 控制聚合粒度，并在后续使用 pHash/ORB 做精细过滤。
 *
 * @author PhotoCleaner
 */
@Singleton
class SimilarityGraph @Inject constructor() {

    /** 内部并查集 */
    private var unionFind: UnionFind = UnionFind(0)

    /** imageId → 内部索引 */
    private val idToIndex: MutableMap<Long, Int> = mutableMapOf()

    /** 内部索引 → imageId */
    private val indexToId: MutableList<Long> = mutableListOf()

    /**
     * 使用已知的图片 ID 集合初始化图。
     *
     * 调用此方法后可通过 [addEdge] 添加相似边。
     *
     * @param imageIds 所有图片 ID 列表
     */
    fun initialize(imageIds: List<Long>) {
        clear()
        val n = imageIds.size
        unionFind = UnionFind(n)
        imageIds.forEachIndexed { index, id ->
            idToIndex[id] = index
            indexToId.add(id)
        }
    }

    /**
     * 在两张图片之间添加一条相似边。
     *
     * 如果 [similarity] >= [threshold]，则将 [imageId1] 和 [imageId2] 所在集合合并。
     *
     * @param imageId1  图片1 ID
     * @param imageId2  图片2 ID
     * @param similarity 相似度分数（0.0 ~ 1.0）
     * @param threshold  判定为相似的阈值
     * @return 如果合并发生则返回 true，否则 false
     */
    fun addEdge(imageId1: Long, imageId2: Long, similarity: Float, threshold: Float): Boolean {
        if (similarity < threshold) return false
        val idx1 = idToIndex[imageId1] ?: return false
        val idx2 = idToIndex[imageId2] ?: return false
        return unionFind.union(idx1, idx2)
    }

    /**
     * 获取当前所有连通分量。
     *
     * @return 映射：根节点 → 该分量中所有图片 ID 列表，仅包含成员数 >= 2 的分量
     */
    fun getConnectedComponents(): Map<Int, List<Long>> {
        val groups = unionFind.getGroups()
        val result = mutableMapOf<Int, MutableList<Long>>()
        for ((root, indices) in groups) {
            if (indices.size >= 2) {
                result[root] = indices.map { indexToId[it] }.toMutableList()
            }
        }
        return result
    }

    /**
     * 清空图，重置所有内部状态。
     */
    fun clear() {
        unionFind = UnionFind(0)
        idToIndex.clear()
        indexToId.clear()
    }
}
