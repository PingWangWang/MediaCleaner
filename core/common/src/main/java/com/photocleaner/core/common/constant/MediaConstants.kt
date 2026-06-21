package com.photocleaner.core.common.constant

/**
 * 媒体相关常量。
 *
 * 定义图片扫描、相似度检测中用到的各类常量和 MIME 类型数组。
 *
 * @author PhotoCleaner
 */
object MediaConstants {

    /** 常见图片 MIME 类型数组 */
    val IMAGE_MIME_TYPES: Array<String> = arrayOf(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/gif",
        "image/bmp",
        "image/heic",
        "image/heif",
        "image/avif"
    )

    /** 最小图片大小：1 KB（小于此值视为无效图片） */
    const val MIN_IMAGE_SIZE: Long = 1024L

    /** 最大图片大小：100 MB（大于此值跳过处理） */
    const val MAX_IMAGE_SIZE: Long = 100L * 1024 * 1024

    /** 缩略图尺寸（像素） */
    const val THUMBNAIL_SIZE: Int = 256

    /** 差异哈希（dHash）尺寸 */
    const val DHASH_SIZE: Int = 8

    /** 感知哈希（pHash）尺寸 */
    const val PHASH_SIZE: Int = 32
}
