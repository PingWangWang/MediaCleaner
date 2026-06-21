package com.photocleaner.feature.duplicate.graph

/**
 * 并查集（Union-Find / Disjoint Set Union）。
 *
 * 支持路径压缩（[find]）和按秩合并（[union]），
 * 用于高效管理不相交集合的合并与查找操作。
 *
 * @param size 元素数量（元素编号为 0 .. size-1）
 * @author PhotoCleaner
 */
class UnionFind(size: Int) {

    /** 父节点数组，初始时每个元素的父节点指向自身 */
    private val parent: IntArray = IntArray(size) { it }

    /** 秩（树高度近似值）数组，用于按秩合并优化 */
    private val rank: IntArray = IntArray(size) { 0 }

    /**
     * 查找元素 [x] 所在集合的根节点。
     *
     * 附带路径压缩：将沿路所有节点的父节点直接指向根节点，
     * 使后续查找接近 O(α(n))。
     *
     * @param x 元素编号
     * @return 根节点编号
     */
    fun find(x: Int): Int {
        if (parent[x] != x) {
            parent[x] = find(parent[x]) // 路径压缩
        }
        return parent[x]
    }

    /**
     * 合并元素 [x] 和 [y] 所在的集合。
     *
     * 按秩合并：将秩较小的树挂到秩较大的树下，避免树高度过度增长。
     *
     * @param x 元素编号
     * @param y 元素编号
     * @return 如果两个元素原本不在同一集合（即发生了合并）则返回 true，否则 false
     */
    fun union(x: Int, y: Int): Boolean {
        val rootX = find(x)
        val rootY = find(y)

        if (rootX == rootY) return false // 已在同一集合

        // 按秩合并
        when {
            rank[rootX] < rank[rootY] -> parent[rootX] = rootY
            rank[rootX] > rank[rootY] -> parent[rootY] = rootX
            else -> {
                parent[rootY] = rootX
                rank[rootX]++
            }
        }

        return true
    }

    /**
     * 获取当前所有连通分量（集合）。
     *
     * @return 映射：根节点 -> 该集合内的所有元素编号列表
     */
    fun getGroups(): Map<Int, List<Int>> {
        val map = mutableMapOf<Int, MutableList<Int>>()
        for (i in parent.indices) {
            val root = find(i)
            map.getOrPut(root) { mutableListOf() }.add(i)
        }
        return map
    }
}
