package com.photocleaner.feature.duplicate.matcher

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OrbFeatureMatcherTest {

    private lateinit var matcher: OrbFeatureMatcher

    @Before
    fun setup() {
        matcher = OrbFeatureMatcher()
    }

    @Test
    fun testMatchSameImageReturnsHighScore() {
        val bitmap = createTestBitmap(100, 100)
        val score = matcher.match(bitmap, bitmap)
        // NCC of identical arrays => 1.0, mapped to (1.0+1)/2 = 1.0
        assertEquals("identical images should match", 1.0f, score, 0.01f)
    }

    @Test
    fun testMatchIdenticalBitmapsReturnsHighScore() {
        val bitmap1 = createTestBitmap(100, 100)
        val bitmap2 = createTestBitmap(100, 100)
        val score = matcher.match(bitmap1, bitmap2)
        // Same pattern => should be near 1.0
        assertTrue("identical patterns score >= 0.99, got $score", score >= 0.99f)
    }

    @Test
    fun testMatchDifferentImagesReturnsLowerScore() {
        val whiteBitmap = createTestBitmap(100, 100, fillWhite = true)
        val blackBitmap = createSolidBitmap(100, 100, 0xFF000000.toInt())
        val score = matcher.match(whiteBitmap, blackBitmap)
        // Checkerboard vs solid black should give a lower score
        assertTrue("different images should score < 1.0, got $score", score < 1.0f)
    }

    @Test
    fun testMatchScoreInZeroToOneRange() {
        val bitmap1 = createTestBitmap(100, 100, fillWhite = true)
        val bitmap2 = createTestBitmap(100, 100, fillWhite = false)
        val score = matcher.match(bitmap1, bitmap2)
        assertTrue("score should be >= 0.0, got $score", score >= 0.0f)
        assertTrue("score should be <= 1.0, got $score", score <= 1.0f)
    }

    @Test
    fun testMatchAllBlackVsAllWhite() {
        val blackBitmap = createSolidBitmap(100, 100, 0xFF000000.toInt())
        val whiteBitmap = createSolidBitmap(100, 100, 0xFFFFFFFF.toInt())
        val score = matcher.match(blackBitmap, whiteBitmap)
        // Solid black vs solid white => different brightness, but both constant
        // NCC of constant arrays gives denominator 0 => mapped to 0f
        assertTrue("black vs white score in range", score in 0.0f..1.0f)
    }

    private fun createTestBitmap(width: Int, height: Int, fillWhite: Boolean = true): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val color = if (fillWhite) {
                    if ((x + y) % 2 == 0) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                } else {
                    if ((x + y) % 3 == 0) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                }
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }

    private fun createSolidBitmap(width: Int, height: Int, color: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }
}
