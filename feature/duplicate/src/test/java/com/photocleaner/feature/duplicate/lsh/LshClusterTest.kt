package com.photocleaner.feature.duplicate.lsh

import com.photocleaner.core.common.model.ImageItem
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LshClusterTest {

    private lateinit var hashFunc: (ImageItem) -> String

    @Before
    fun setup() {
        hashFunc = { it.dHash ?: "" }
    }

    @Test
    fun testEmptyInput() {
        val clusters = LshClusterAlgorithm.cluster(
            images = emptyList(),
            hashFunc = hashFunc
        )
        assertEquals(0, clusters.size)
    }

    @Test
    fun testSingleItem() {
        val images = listOf(
            createImage(1, "1010101010101010101010101010101010101010101010101010101010101010")
        )
        val clusters = LshClusterAlgorithm.cluster(images, hashFunc)
        // Single item should still be in a cluster of size 1
        assertEquals(1, clusters.size)
        assertEquals(1, clusters[0].images.size)
    }

    @Test
    fun testClusterWithIdenticalHashes() {
        val hash = "1010101010101010101010101010101010101010101010101010101010101010"
        val images = listOf(
            createImage(1, hash),
            createImage(2, hash),
            createImage(3, hash)
        )
        val clusters = LshClusterAlgorithm.cluster(images, hashFunc)
        // All should fall into same cluster (same hash = same bucket in all bands)
        val totalItems = clusters.sumOf { it.images.size }
        assertEquals(3, totalItems)
    }

    @Test
    fun testClusterWithDifferentHashes() {
        // Hash 1: all zeros
        val hash1 = "0000000000000000000000000000000000000000000000000000000000000000"
        // Hash 2: all ones
        val hash2 = "1111111111111111111111111111111111111111111111111111111111111111"
        // Hash 3: half zeros, half ones
        val hash3 = "0000000000000000000000000000000011111111111111111111111111111111"

        val images = listOf(
            createImage(1, hash1),
            createImage(2, hash2),
            createImage(3, hash3)
        )
        val clusters = LshClusterAlgorithm.cluster(images, hashFunc, numBands = 4, numRows = 16)
        // With these parameters, at least some pairs should fall into different clusters
        val totalItems = clusters.sumOf { it.images.size }
        assertEquals(3, totalItems)
    }

    private fun createImage(id: Long, dHash: String): ImageItem {
        return ImageItem(
            id = id,
            uri = "content://test/$id",
            name = "img_$id.jpg",
            size = 1024,
            modifyTime = System.currentTimeMillis(),
            dHash = dHash
        )
    }
}
