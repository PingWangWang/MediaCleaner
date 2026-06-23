package com.photocleaner.feature.duplicate.hash

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

/**
 * DHashCalculator 的插桩测试。
 * 在设备/模拟器上运行，使用真实的 android.graphics.Bitmap。
 */
class DHashCalculatorTest {

    private lateinit var calculator: DHashCalculator

    @Before
    fun setup() {
        calculator = DHashCalculator()
    }

    @Test
    fun testCalculateHashReturns64BitString() {
        val bitmap = createTestBitmap(100, 100)
        val hash = calculator.calculateHash(bitmap)
        assertEquals(64, hash.length)
    }

    @Test
    fun testCalculateHashSameImageReturnsSameHash() {
        val bitmap = createTestBitmap(100, 100)
        val hash1 = calculator.calculateHash(bitmap)
        val hash2 = calculator.calculateHash(bitmap)
        assertEquals(hash1, hash2)
    }

    @Test
    fun testCalculateHashDifferentImagesReturnDifferentHashes() {
        val whiteBitmap = createTestBitmap(100, 100, fillWhite = true)
        val blackBitmap = createTestBitmap(100, 100, fillWhite = false)
        val hashWhite = calculator.calculateHash(whiteBitmap)
        val hashBlack = calculator.calculateHash(blackBitmap)
        assertNotEquals(hashWhite, hashBlack)
    }

    private fun createTestBitmap(width: Int, height: Int, fillWhite: Boolean = true): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val color = if (fillWhite) {
                    if ((x + y) % 2 == 0) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
                } else {
                    0xFF000000.toInt()
                }
                bitmap.setPixel(x, y, color)
            }
        }
        return bitmap
    }
}
