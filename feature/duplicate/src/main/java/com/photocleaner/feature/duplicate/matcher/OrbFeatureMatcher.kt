/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * ORB 特征匹配器
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.duplicate.matcher

import android.graphics.Bitmap
import com.photocleaner.core.common.constant.MediaConstants
import com.photocleaner.core.common.extension.resize
import com.photocleaner.core.common.extension.toGrayscale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * ORB 特征匹配器（简化实现）。
 *
 * 为避免引入 OpenCV 依赖，本实现采用归一化互相关（NCC）作为近似，
 * 将两张图片缩放到相同尺寸、灰度化后逐像素计算相关性。
 *
 * ORB 启用阈值与三门控条件用于决定何时值得执行 ORB 级精细匹配。
 *
 * @author PhotoCleaner
 */
@Singleton
class OrbFeatureMatcher @Inject constructor() {

    /** ORB 精细化匹配的启用阈值：仅在 dHash 相似度处于"灰色地带"时启用 */
    companion object {
        const val ORB_ENABLE_THRESHOLD = 0.75f
    }

    /**
     * 匹配两张位图并返回相似度分数。
     *
     * 实现方式（简化）：
     * 1. 将两张图片缩放到统一尺寸（THUMBNAIL_SIZE × THUMBNAIL_SIZE）。
     * 2. 灰度化。
     * 3. 提取亮度数组并归一化。
     * 4. 计算归一化互相关（NCC）系数，映射到 [0, 1] 区间。
     *
     * @param bitmap1 第一张位图
     * @param bitmap2 第二张位图
     * @return 相似度分数，范围 0.0（完全不同）～ 1.0（完全相同）
     */
    fun match(bitmap1: Bitmap, bitmap2: Bitmap): Float {
        val size = MediaConstants.THUMBNAIL_SIZE

        // 1) 缩放到统一尺寸
        val resized1 = bitmap1.resize(size, size)
        val resized2 = bitmap2.resize(size, size)

        // 2) 灰度化
        val gray1 = resized1.toGrayscale()
        val gray2 = resized2.toGrayscale()

        // 3) 提取亮度数组
        val pixels1 = IntArray(size * size)
        val pixels2 = IntArray(size * size)
        gray1.getPixels(pixels1, 0, size, 0, 0, size, size)
        gray2.getPixels(pixels2, 0, size, 0, 0, size, size)

        val brightness1 = pixels1.map { brightness(it).toFloat() }
        val brightness2 = pixels2.map { brightness(it).toFloat() }

        // 4) 计算归一化互相关
        return normalizedCrossCorrelation(brightness1, brightness2)
    }

    /**
     * 计算两个 Float 序列的归一化互相关（NCC）系数。
     *
     * NCC 范围 [-1, 1]，此处映射到 [0, 1]。
     */
    private fun normalizedCrossCorrelation(a: List<Float>, b: List<Float>): Float {
        val n = a.size
        if (n == 0) return 0f

        val meanA = a.sum() / n
        val meanB = b.sum() / n

        var numerator = 0.0
        var denomA = 0.0
        var denomB = 0.0

        for (i in 0 until n) {
            val diffA = a[i] - meanA
            val diffB = b[i] - meanB
            numerator += diffA * diffB
            denomA += diffA * diffA
            denomB += diffB * diffB
        }

        val denominator = sqrt(denomA) * sqrt(denomB)
        if (denominator == 0.0) return 0f

        // 将 [-1, 1] 映射到 [0, 1]
        return ((numerator / denominator).toFloat() + 1f) / 2f
    }

    /**
     * 从 ARGB 像素中提取亮度值。
     */
    private fun brightness(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }
}
