package com.photocleaner.feature.duplicate.hash

import android.graphics.Bitmap
import com.photocleaner.core.common.constant.MediaConstants
import com.photocleaner.core.common.extension.resize
import com.photocleaner.core.common.extension.toGrayscale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * 感知哈希（pHash）计算器。
 *
 * 采用离散余弦变换（DCT）方法：
 * 1. 将图片缩放到 32×32 并灰度化。
 * 2. 对整个 32×32 像素矩阵执行二维 DCT。
 * 3. 从左上角截取 8×8 的低频系数区域。
 * 4. 以该 8×8 区域的 DCT 中位数为阈值生成 64 位哈希。
 *
 * @author PhotoCleaner
 */
@Singleton
class PHashCalculator @Inject constructor() {

    /** pHash 所需的正方形边长 */
    private val pHashSize = MediaConstants.PHASH_SIZE

    /** DCT 截取区域边长（取左上角低频部分） */
    private val dctRegionSize = MediaConstants.DHASH_SIZE // 8

    /**
     * 计算给定 [bitmap] 的 pHash 值。
     *
     * @param bitmap 输入位图
     * @return 64 位二进制字符串，仅包含 '0' 和 '1'
     */
    fun calculateHash(bitmap: Bitmap): String {
        // 1) 缩放到 PHASH_SIZE × PHASH_SIZE 并灰度化
        val resized = bitmap.resize(pHashSize, pHashSize)
        val gray = resized.toGrayscale()

        // 2) 提取灰度像素矩阵
        val pixels = IntArray(pHashSize * pHashSize)
        gray.getPixels(pixels, 0, pHashSize, 0, 0, pHashSize, pHashSize)

        val matrix = Array(pHashSize) { y ->
            DoubleArray(pHashSize) { x ->
                brightness(pixels[y * pHashSize + x]).toDouble()
            }
        }

        // 3) 二维 DCT
        val dct = applyDCT(matrix)

        // 4) 取左上角 8×8 低频区域
        val topLeft = Array(dctRegionSize) { y ->
            DoubleArray(dctRegionSize) { x ->
                dct[y][x]
            }
        }

        // 5) 计算中位数
        val values = topLeft.flatMap { it.asList() }.toDoubleArray()
        values.sort()
        val median = values[values.size / 2]

        // 6) 以中位数为阈值生成哈希
        val sb = StringBuilder(dctRegionSize * dctRegionSize)
        for (y in 0 until dctRegionSize) {
            for (x in 0 until dctRegionSize) {
                sb.append(if (topLeft[y][x] >= median) '1' else '0')
            }
        }

        return sb.toString()
    }

    /**
     * 对 N×N 矩阵执行二维离散余弦变换。
     *
     * 使用 DCT-II 公式（标准正交归一化版本），复杂度 O(N³)。
     * 由于 N=32 且在后台线程执行，性能可接受。
     */
    private fun applyDCT(matrix: Array<DoubleArray>): Array<DoubleArray> {
        val n = matrix.size
        val result = Array(n) { DoubleArray(n) }

        val factor = sqrt(2.0 / n)
        val cosTable = Array(n) { p ->
            DoubleArray(n) { q ->
                cos((Math.PI * (2 * q + 1) * p) / (2 * n))
            }
        }

        for (u in 0 until n) {
            for (v in 0 until n) {
                var sum = 0.0
                for (x in 0 until n) {
                    for (y in 0 until n) {
                        sum += matrix[x][y] * cosTable[u][x] * cosTable[v][y]
                    }
                }
                result[u][v] = sum * factor * cu(u) * cu(v)
            }
        }

        return result
    }

    /**
     * DCT 归一化系数：C(0) = 1/√2，其它为 1。
     */
    private fun cu(index: Int): Double {
        return if (index == 0) 1.0 / sqrt(2.0) else 1.0
    }

    /**
     * 从 ARGB 像素中提取亮度值。
     */
    private fun brightness(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b).roundToInt()
    }
}
