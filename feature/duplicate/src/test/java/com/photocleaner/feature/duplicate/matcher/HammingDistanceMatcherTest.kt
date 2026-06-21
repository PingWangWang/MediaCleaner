package com.photocleaner.feature.duplicate.matcher

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HammingDistanceMatcherTest {

    private lateinit var matcher: HammingDistanceMatcher

    @Before
    fun setup() {
        matcher = HammingDistanceMatcher()
    }

    @Test
    fun testExactMatch() {
        val hash = "0101010101010101010101010101010101010101010101010101010101010101"
        assertEquals(0, matcher.computeHammingDistance(hash, hash))
    }

    @Test
    fun testSingleBitDifference() {
        val hash1 = "0101010101010101010101010101010101010101010101010101010101010101"
        val hash2 = "0101010101010101010101010101010101010101010101010101010101010100"
        assertEquals(1, matcher.computeHammingDistance(hash1, hash2))
    }

    @Test
    fun testAllBitsDifferent() {
        val hash1 = "0000000000000000000000000000000000000000000000000000000000000000"
        val hash2 = "1111111111111111111111111111111111111111111111111111111111111111"
        assertEquals(64, matcher.computeHammingDistance(hash1, hash2))
    }

    @Test
    fun testIsExactMatch() {
        val hash = "1010101010101010101010101010101010101010101010101010101010101010"
        assertTrue(matcher.isExactMatch(hash, hash))
        assertFalse(matcher.isExactMatch(hash, hash.replaceFirst('1', '0')))
    }

    @Test
    fun testIsHighSimilarity() {
        val hash1 = "0000000000000000000000000000000000000000000000000000000000000000"
        val hash2 = "0000000000000001000000000000000000000000000000000000000000000000"
        assertTrue(matcher.isHighSimilarity(hash1, hash2, 10))
        assertFalse(matcher.isHighSimilarity(hash1, hash2, 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testThrowsOnDifferentLengths() {
        matcher.computeHammingDistance("short", "different-length-string")
    }

    @Test
    fun testHalfBitsDifferent() {
        val hash1 = "0000000000000000000000000000000011111111111111111111111111111111"
        val hash2 = "1111111111111111111111111111111100000000000000000000000000000000"
        assertEquals(64, matcher.computeHammingDistance(hash1, hash2))
    }
}
