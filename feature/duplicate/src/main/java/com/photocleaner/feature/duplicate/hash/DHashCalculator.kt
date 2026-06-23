/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 差异哈希(dHash)计算器
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.duplicate.hash

import android.graphics.Bitmap
import com.photocleaner.core.common.constant.MediaConstants
import com.photocleaner.core.common.extension.resize
import com.photocleaner.core.common.extension.toGrayscale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 差异哈希（dHash）计算器。
 *
 * 将图片缩小到 9×8 像素并灰度化后，逐行比较相邻像素的亮度，
 * 左 > 右 记为 1，否则记为 0，最终生成 64 位二进制哈希指纹。
 *
 * @author PhotoCleaner
 */
@Singleton
class DHashCalculator @Inject constructor() {

    /**
     * 计算给定 [bitmap] 的 dHash 值。
     *
     * @param bitmap 输入位图
     * @return 64 位二进制字符串，仅包含 '0' 和 '1'
     */
    fun calculateHash(bitmap: Bitmap): String {
        val thumbSize = MediaConstants.THUMBNAIL_SIZE
        val dHashSize = MediaConstants.DHASH_SIZE

        // 1) 缩放到统一尺寸再转为 9×8
        val resized = bitmap
            .resize(thumbSize, thumbSize)
            .resize(dHashSize + 1, dHashSize)

        // 2) 灰度化
        val gray = resized.toGrayscale()

        // 3) 提取像素亮度，比较相邻列
        val pixels = IntArray(gray.width * gray.height)
        gray.getPixels(pixels, 0, gray.width, 0, 0, gray.width, gray.height)

        val sb = StringBuilder(dHashSize * dHashSize)
        for (y in 0 until dHashSize) {
            for (x in 0 until dHashSize) {
                val left = brightness(pixels[y * gray.width + x])
                val right = brightness(pixels[y * gray.width + (x + 1)])
                sb.append(if (left >= right) '0' else '1')
            }
        }

        return sb.toString()
    }

    /**
     * 从 ARGB 像素中提取亮度值（灰度强度）。
     */
    private fun brightness(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }
}
