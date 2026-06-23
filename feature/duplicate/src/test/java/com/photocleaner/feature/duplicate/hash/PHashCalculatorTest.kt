package com.photocleaner.feature.duplicate.hash

import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PHashCalculatorTest {

    private lateinit var calculator: PHashCalculator

    @Before
    fun setup() {
        calculator = PHashCalculator()
    }

    @Test
    fun testCalculateHashSameImageReturnsSameHash() {
        val bitmap1 = createTestBitmap(64, 64)
        val bitmap2 = createTestBitmap(64, 64)

        val hash1 = calculator.calculateHash(bitmap1)
        val hash2 = calculator.calculateHash(bitmap2)

        assertEquals(hash1, hash2)
    }

    @Test
    fun testCalculateHashDifferentImagesReturnDifferentHashes() {
        val bitmap1 = createTestBitmap(64, 64, fillWhite = true)
        val bitmap2 = createTestBitmap(64, 64, fillWhite = false)

        val hash1 = calculator.calculateHash(bitmap1)
        val hash2 = calculator.calculateHash(bitmap2)

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testCalculateHashReturns64BitString() {
        val bitmap = createTestBitmap(64, 64)
        val hash = calculator.calculateHash(bitmap)

        // PHashCalculator uses 8x8 DCT region => 64 bits
        assertEquals(64, hash.length)
        assertTrue(hash.all { it == '0' || it == '1' })
    }

    @Test
    fun testCalculateHashDifferentImageSizesProduceSameLength() {
        val smallBitmap = createTestBitmap(32, 32)
        val largeBitmap = createTestBitmap(200, 200)

        val hash1 = calculator.calculateHash(smallBitmap)
        val hash2 = calculator.calculateHash(largeBitmap)

        assertEquals(64, hash1.length)
        assertEquals(64, hash2.length)
    }

    @Test
    fun testCalculateHashAllBlackAndAllWhiteAreDifferent() {
        val blackBitmap = createSolidBitmap(64, 64, 0xFF000000.toInt())
        val whiteBitmap = createSolidBitmap(64, 64, 0xFFFFFFFF.toInt())

        val hash1 = calculator.calculateHash(blackBitmap)
        val hash2 = calculator.calculateHash(whiteBitmap)

        assertNotEquals(hash1, hash2)
        assertEquals(64, hash1.length)
        assertEquals(64, hash2.length)
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
