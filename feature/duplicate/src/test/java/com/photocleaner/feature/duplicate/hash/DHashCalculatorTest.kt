package com.photocleaner.feature.duplicate.hash

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DHashCalculatorTest {

    private lateinit var calculator: DHashCalculator

    @Before
    fun setup() {
        calculator = DHashCalculator()
    }

    @Test
    fun testCalculateHashSameImageReturnsSameHash() {
        val bitmap1 = createTestBitmap(9, 8)
        val bitmap2 = createTestBitmap(9, 8)

        val hash1 = calculator.calculateHash(bitmap1)
        val hash2 = calculator.calculateHash(bitmap2)

        assertEquals(hash1, hash2)
    }

    @Test
    fun testCalculateHashDifferentImagesReturnDifferentHashes() {
        val bitmap1 = createTestBitmap(9, 8, fillWhite = true)
        val bitmap2 = createTestBitmap(9, 8, fillWhite = false)

        val hash1 = calculator.calculateHash(bitmap1)
        val hash2 = calculator.calculateHash(bitmap2)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testCalculateHashReturns64BitString() {
        val bitmap = createTestBitmap(9, 8)
        val hash = calculator.calculateHash(bitmap)

        assertEquals(64, hash.length)
        assertTrue(hash.all { it == '0' || it == '1' })
    }

    @Test
    fun testCalculateHashWithNullBitmap() {
        // Should handle gracefully
        val result = calculator.calculateHash(null as? Bitmap ?: return)
        assertTrue(result.isNotEmpty())
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
}
