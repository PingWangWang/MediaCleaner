package com.photocleaner.core.common.extension

import android.webkit.MimeTypeMap
import java.io.File
import java.text.DecimalFormat

/**
 * File 与 Long 扩展函数集合。
 *
 * 提供文件大小格式化、MIME 类型推断等实用功能。
 *
 * @author PhotoCleaner
 */

/**
 * 将文件大小格式化为人类可读的字符串。
 *
 * 自动选择 B / KB / MB / GB / TB 单位，保留一位小数。
 *
 * 例如：1024 → "1.0 KB"，1536 → "1.5 KB"，1048576 → "1.0 MB"
 *
 * @return 格式化后的大小字符串，如 "2.5 MB"
 */
fun File.formatSize(): String {
    return length().toFileSize()
}

/**
 * 根据文件扩展名获取 MIME 类型。
 *
 * 内部使用 [MimeTypeMap.getSingleton().getMimeTypeFromExtension] 进行查询。
 * 若无法识别则返回 "application/octet-stream"。
 *
 * @return 对应的 MIME 类型字符串
 */
fun File.getMimeType(): String {
    val extension = extension.lowercase()
    return MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extension)
        ?: "application/octet-stream"
}

/**
 * 将字节数（Long）格式化为人类可读的大小字符串。
 *
 * 自动选择 B / KB / MB / GB / TB 单位，保留一位小数。
 *
 * 例如：
 * - 500 → "500 B"
 * - 2048 → "2.0 KB"
 * - 1048576 → "1.0 MB"
 *
 * @return 格式化后的大小字符串
 */
fun Long.toFileSize(): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = this.toDouble()
    var unitIndex = 0

    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }

    return when {
        unitIndex == 0 -> "${this} B"
        else -> {
            val df = DecimalFormat("#.#")
            "${df.format(size)} ${units[unitIndex]}"
        }
    }
}
