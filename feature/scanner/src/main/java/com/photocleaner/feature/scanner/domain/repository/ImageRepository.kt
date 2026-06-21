package com.photocleaner.feature.scanner.domain.repository

import com.photocleaner.core.common.model.ImageItem
import kotlinx.coroutines.flow.Flow

/**
 * 图片仓库接口。
 *
 * 定义图片扫描、持久化、查询和更新操作的契约。
 * 实现类将协调 MediaStore / SAF 数据源与本地数据库之间的数据流转。
 *
 * @author PhotoCleaner
 */
interface ImageRepository {

    /**
     * 扫描所有图片（全量扫描）。
     *
     * 从 MediaStore 查询所有图片，逐个发射 [ImageItem]。
     *
     * @return Flow 逐个发射扫描到的图片
     */
    fun scanAllImages(): Flow<ImageItem>

    /**
     * 增量扫描指定时间之后修改过的图片。
     *
     * @param since 时间戳（毫秒），仅返回修改时间大于此值的图片
     * @return Flow 逐个发射扫描到的图片
     */
    fun scanIncremental(since: Long): Flow<ImageItem>

    /**
     * 批量保存图片到本地数据库。
     *
     * @param images 待保存的图片列表
     */
    suspend fun saveImages(images: List<ImageItem>)

    /**
     * 根据 ID 获取单张图片。
     *
     * @param id 图片 ID
     * @return 匹配的图片，或 null
     */
    suspend fun getImageById(id: Long): ImageItem?

    /**
     * 根据大小分桶和宽高比分桶查询图片。
     *
     * 用于重复检测的候选预过滤。
     *
     * @param sizeBucket  大小分桶索引
     * @param ratioBucket 宽高比分桶索引
     * @return 匹配的图片列表
     */
    fun getImagesByBuckets(sizeBucket: Int, ratioBucket: Int): List<ImageItem>

    /**
     * 获取尚未计算哈希的图片。
     *
     * @param limit 返回的最大数量
     * @return 未计算哈希的图片列表
     */
    fun getUncalculatedImages(limit: Int): List<ImageItem>

    /**
     * 更新指定图片的哈希指纹。
     *
     * @param id    图片 ID
     * @param dHash 差异哈希值（可为 null）
     * @param pHash 感知哈希值（可为 null）
     */
    suspend fun updateImageHashes(id: Long, dHash: String?, pHash: String?)

    /**
     * 批量删除图片记录。
     *
     * @param ids 待删除的图片 ID 列表
     */
    suspend fun deleteImages(ids: List<Long>)
}
