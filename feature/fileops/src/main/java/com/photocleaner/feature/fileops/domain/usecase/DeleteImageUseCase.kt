/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 删除图片用例
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.fileops.domain.usecase

import com.photocleaner.core.common.model.ImageItem
import com.photocleaner.feature.fileops.domain.FileOperator
import com.photocleaner.feature.fileops.model.DeleteResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 删除单张图片的用例。
 *
 * 封装了从触发删除到返回结果的完整流程，通过 [Flow] 向外发射执行状态：
 * 1. [DeleteStatus.DELETING] — 删除进行中
 * 2. [DeleteStatus.DELETED] 或 [DeleteStatus.FAILED] — 最终结果
 *
 * 调用方可收集此 Flow 来驱动 UI 状态（如显示加载动画、成功/失败提示）。
 *
 * @property fileOperator 文件操作接口，实际执行删除逻辑
 *
 * @author PhotoCleaner
 */
@Singleton
class DeleteImageUseCase @Inject constructor(
    private val fileOperator: FileOperator
) {

    /**
     * 执行删除操作。
     *
     * @param image 待删除的 [ImageItem]
     * @return 发射 [DeleteStatus] 的 Flow，按 DELETING → DELETED / FAILED 顺序
     */
    operator fun invoke(image: ImageItem): Flow<DeleteStatus> = callbackFlow {
        // 第一步：发射"删除中"状态
        trySend(DeleteStatus.DELETING(imageId = image.id))

        // 第二步：执行实际的删除操作
        val job = launch {
            val result = fileOperator.deleteImage(image)
            when (result) {
                is DeleteResult.SUCCESS -> {
                    trySend(DeleteStatus.DELETED(imageId = result.imageId, savedBytes = result.savedBytes))
                }
                is DeleteResult.FAILED -> {
                    trySend(DeleteStatus.FAILED(imageId = result.imageId, errorMessage = result.errorMessage))
                }
            }
            close()
        }

        // 当 Flow 被取消时，取消协程
        awaitClose {
            job.cancel()
        }
    }
}

/**
 * 删除操作的状态标志。
 *
 * 用于驱动 UI 层展示删除进度。
 */
sealed class DeleteStatus {

    /**
     * 删除执行中。
     *
     * @property imageId 正在删除的图片 ID
     */
    data class DELETING(val imageId: Long) : DeleteStatus()

    /**
     * 删除成功。
     *
     * @property imageId    被删除图片的 ID
     * @property savedBytes 释放的存储空间字节数
     */
    data class DELETED(val imageId: Long, val savedBytes: Long) : DeleteStatus()

    /**
     * 删除失败。
     *
     * @property imageId      尝试删除的图片 ID
     * @property errorMessage 失败原因
     */
    data class FAILED(val imageId: Long, val errorMessage: String) : DeleteStatus()
}
