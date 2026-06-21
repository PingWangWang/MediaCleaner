package com.photocleaner.core.common.utils

import java.text.DecimalFormat

/**
 * 文件大小转换工具类。
 *
 * 提供字节数与 KB/MB 之间的换算以及人类可读格式化输出。
 *
 * @author PhotoCleaner
 */
object SizeUtils {

    private const val BYTES_IN_KB = 1024L
    private const val BYTES_IN_MB = 1024L * 1024
    private const val BYTES_IN_GB = 1024L * 1024 * 1024

    private val decimalFormat = DecimalFormat("#.#")

    /**
     * 将字节数格式化为人类可读的大小字符串（中文单位）。
     *
     * 自动选择 B / KB / MB / GB 单位，保留一位小数。
     *
     * 例如：
     * - 500     → "500 B"
     * - 2048    → "2.0 KB"
     * - 1048576 → "1.0 MB"
     *
     * @param bytes 字节数
     * @return 格式化后的大小字符串
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < BYTES_IN_KB -> "$bytes B"
            bytes < BYTES_IN_MB -> "${decimalFormat.format(bytes.toDouble() / BYTES_IN_KB)} KB"
            bytes < BYTES_IN_GB -> "${decimalFormat.format(bytes.toDouble() / BYTES_IN_MB)} MB"
            else -> "${decimalFormat.format(bytes.toDouble() / BYTES_IN_GB)} GB"
        }
    }

    /**
     * 将字节数转换为 KB（向下取整）。
     *
     * @param bytes 字节数
     * @return KB 值
     */
    fun bytesToKB(bytes: Long): Long {
        return bytes / BYTES_IN_KB
    }

    /**
     * 将字节数转换为 MB（浮点数）。
     *
     * @param bytes 字节数
     * @return MB 值（保留浮点精度）
     */
    fun bytesToMB(bytes: Long): Float {
        return bytes.toFloat() / BYTES_IN_MB
    }
}
