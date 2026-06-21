package com.photocleaner.core.common.model

/**
 * 图片项数据模型。
 *
 * 代表 MediaStore 中一张图片的元信息，包括文件路径、尺寸、哈希指纹等，
 * 用于后续的重复图片检测与相似度比对。
 *
 * @property id           MediaStore 中的图片 ID
 * @property uri          图片的 Content URI 字符串
 * @property path         图片文件路径（可能为 null）
 * @property name         图片文件名称
 * @property size         文件大小（字节）
 * @property width        图片宽度（像素），可能为 null
 * @property height       图片高度（像素），可能为 null
 * @property mimeType     MIME 类型，可能为 null
 * @property modifyTime   文件最后修改时间（毫秒）
 * @property dHash        差异哈希值（64 位十六进制字符串），初始为 null
 * @property pHash        感知哈希值（1024 位十六进制字符串），初始为 null
 * @property sizeBucket   大小分桶索引，用于初步快速分组
 * @property ratioBucket  宽高比分桶索引，用于初步快速分组
 * @property orientation  图片旋转角度（0 / 90 / 180 / 270）
 * @property isCalculated 是否已完成哈希计算
 * @property scanTime     本条记录的扫描时间戳，默认为当前时间
 *
 * @author PhotoCleaner
 */
data class ImageItem(
    val id: Long,
    val uri: String,
    val path: String? = null,
    val name: String,
    val size: Long,
    val width: Int? = null,
    val height: Int? = null,
    val mimeType: String? = null,
    val modifyTime: Long,
    val dHash: String? = null,
    val pHash: String? = null,
    val sizeBucket: Int = 0,
    val ratioBucket: Int = 0,
    val orientation: Int = 0,
    val isCalculated: Boolean = false,
    val scanTime: Long = System.currentTimeMillis()
)
