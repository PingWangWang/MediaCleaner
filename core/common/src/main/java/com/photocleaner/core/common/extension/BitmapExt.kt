package com.photocleaner.core.common.extension

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Canvas
import java.io.ByteArrayOutputStream

/**
 * Bitmap 扩展函数集合。
 *
 * 提供图片灰度转换、尺寸缩放、字节数组导出等常用操作。
 *
 * @author PhotoCleaner
 */

/**
 * 将当前 Bitmap 转换为灰度图。
 *
 * 通过 [ColorMatrix] 将 RGB 通道按亮度权重（0.299R + 0.587G + 0.114B）混合，
 * 保留原始尺寸与透明度信息。
 *
 * @return 灰度化后的新 Bitmap，若转换失败则返回原图
 */
fun Bitmap.toGrayscale(): Bitmap {
    return try {
        val width = width
        val height = height
        val grayscaleBitmap = Bitmap.createBitmap(width, height, config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(grayscaleBitmap)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(
                ColorMatrix().apply {
                    setSaturation(0f)
                }
            )
        }
        canvas.drawBitmap(this, 0f, 0f, paint)
        grayscaleBitmap
    } catch (e: Exception) {
        this
    }
}

/**
 * 将当前 Bitmap 缩放到指定尺寸。
 *
 * 使用 [Bitmap.createScaledBitmap] 进行缩放，[filter] 参数控制是否开启双线性过滤。
 *
 * @param width  目标宽度（像素）
 * @param height 目标高度（像素）
 * @param filter 是否启用双线性过滤，默认为 true
 * @return 缩放后的新 Bitmap
 */
fun Bitmap.resize(width: Int, height: Int, filter: Boolean = true): Bitmap {
    return Bitmap.createScaledBitmap(this, width, height, filter)
}

/**
 * 将当前 Bitmap 按指定压缩格式导出为字节数组。
 *
 * @param compressFormat 压缩格式，如 [Bitmap.CompressFormat.JPEG]、[Bitmap.CompressFormat.PNG]、
 *                       [Bitmap.CompressFormat.WEBP]
 * @param quality       压缩质量（0~100），默认为 80。无损格式（如 PNG）会忽略此参数
 * @return 压缩后的字节数组
 */
fun Bitmap.toByteArray(
    compressFormat: Bitmap.CompressFormat,
    quality: Int = 80
): ByteArray {
    val outputStream = ByteArrayOutputStream()
    compress(compressFormat, quality, outputStream)
    return outputStream.toByteArray()
}
