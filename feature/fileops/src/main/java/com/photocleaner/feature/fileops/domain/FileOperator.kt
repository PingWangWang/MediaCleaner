package com.photocleaner.feature.fileops.domain

import com.photocleaner.core.database.entity.RecycleItemEntity
import com.photocleaner.core.common.model.ImageItem
import com.photocleaner.feature.fileops.model.DeleteResult
import kotlinx.coroutines.flow.Flow

/**
 * 文件操作接口，定义图片删除与回收站管理的核心契约。
 *
 * 所有数据操作均通过此接口完成，上层业务逻辑（UseCase）仅依赖此抽象，
 * 而不关心底层实现（MediaStore 操作、文件 I/O、数据库事务等）。
 *
 * @author PhotoCleaner
 */
interface FileOperator {

    /**
     * 删除单张图片。
     *
     * 流程：
     * 1. 将原始文件移入应用私有回收目录
     * 2. 在回收站数据表中记录一条待恢复/过期记录
     * 3. 返回 [DeleteResult] 指示成功或失败
     *
     * @param image 要删除的 [ImageItem]
     * @return [DeleteResult.SUCCESS] 或 [DeleteResult.FAILED]
     */
    suspend fun deleteImage(image: ImageItem): DeleteResult

    /**
     * 批量删除多张图片。
     *
     * 逐张调用 [deleteImage]，收集并返回每条结果。调用方可通过结果列表
     * 统计成功 / 失败数量。
     *
     * @param images 待删除的图片列表
     * @return 与输入列表一一对应的 [DeleteResult] 列表
     */
    suspend fun deleteImages(images: List<ImageItem>): List<DeleteResult>

    /**
     * 从回收站恢复一张图片到原始位置。
     *
     * 将回收目录中的文件移动回原始 URI 对应路径，并删除回收站数据记录。
     *
     * @param recycleItemId 回收站记录的主键 ID
     * @return true 恢复成功，false 恢复失败（记录不存在或文件 I/O 错误）
     */
    suspend fun restoreImage(recycleItemId: Long): Boolean

    /**
     * 获取回收站中的所有条目。
     *
     * 返回 [Flow]，当回收站数据发生变化时可自动通知上游观察者刷新列表。
     *
     * @return 发射 [RecycleItemEntity] 列表的 Flow
     */
    fun getRecycleBinItems(): Flow<List<RecycleItemEntity>>

    /**
     * 清除所有已过期的回收站条目。
     *
     * 删除数据库中 expire_time < 当前时间 的记录，同时删除对应的回收目录文件。
     * 通常由 [AutoClearRecycleUseCase] 定期调用。
     */
    suspend fun clearExpiredItems()

    /**
     * 永久删除一条回收站记录及其对应的文件。
     *
     * 与 [clearExpiredItems] 不同，此方法由用户手动触发，删除任意指定条目。
     *
     * @param itemId 回收站记录的主键 ID
     */
    suspend fun permanentlyDelete(itemId: Long)

    /**
     * 获取回收站中当前条目数量。
     *
     * 以 [Flow] 形式提供，可用于 UI 层显示 Badge 或角标计数。
     *
     * @return 发射回收站条目总数的 Flow
     */
    fun getRecycleCount(): Flow<Int>
}
