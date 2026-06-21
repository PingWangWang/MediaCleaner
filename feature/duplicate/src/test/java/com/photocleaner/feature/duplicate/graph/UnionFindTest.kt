package com.photocleaner.feature.duplicate.graph

import org.junit.Assert.*
import org.junit.Test

class UnionFindTest {

    @Test
    fun testInitialState() {
        val uf = UnionFind(5)
        for (i in 0 until 5) {
            assertEquals(i, uf.find(i))
        }
    }

    @Test
    fun testUnionTwoElements() {
        val uf = UnionFind(5)
        uf.union(0, 1)
        assertEquals(uf.find(0), uf.find(1))
        assertNotEquals(uf.find(0), uf.find(2))
    }

    @Test
    fun testPathCompression() {
        val uf = UnionFind(10)
        uf.union(0, 1)
        uf.union(1, 2)
        uf.union(2, 3)
        uf.union(3, 4)
        // After path compression, find should directly point to root
        val root = uf.find(4)
        assertEquals(root, uf.find(0))
        assertEquals(root, uf.find(1))
        assertEquals(root, uf.find(2))
        assertEquals(root, uf.find(3))
    }

    @Test
    fun testGetGroups() {
        val uf = UnionFind(6)
        // Group 1: {0, 1, 2}
        uf.union(0, 1)
        uf.union(1, 2)
        // Group 2: {3, 4}
        uf.union(3, 4)

        val groups = uf.getGroups()
        assertEquals(3, groups.size) // 3 groups: {0,1,2}, {3,4}, {5}

        // Verify group sizes
        val groupSizes = groups.values.map { it.size }.sorted()
        assertEquals(listOf(1, 2, 3), groupSizes)
    }

    @Test
    fun testComplexUnion() {
        val uf = UnionFind(8)
        uf.union(0, 1)
        uf.union(2, 3)
        uf.union(4, 5)
        uf.union(6, 7)
        uf.union(0, 2) // Merges {0,1} with {2,3}
        uf.union(4, 6) // Merges {4,5} with {6,7}

        val groups = uf.getGroups()
        assertEquals(2, groups.size) // {0,1,2,3} and {4,5,6,7}

        val root0 = uf.find(0)
        val root4 = uf.find(4)
        assertNotEquals(root0, root4)
    }
}
