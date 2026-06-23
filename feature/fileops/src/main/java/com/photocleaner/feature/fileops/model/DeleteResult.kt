/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 删除操作结果模型
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.fileops.model

/**
 * 删除操作的结果封装。
 *
 * 使用密封类确保每个删除结果要么是成功（附带释放的空间字节数），
 * 要么是失败（附带错误信息），便于调用方通过 when 表达式完整处理。
 *
 * @author PhotoCleaner
 */
sealed class DeleteResult {

    /**
     * 删除成功。
     *
     * @property imageId     被删除图片的 MediaStore ID
     * @property savedBytes  释放的存储空间字节数
     */
    data class SUCCESS(
        val imageId: Long,
        val savedBytes: Long
    ) : DeleteResult()

    /**
     * 删除失败。
     *
     * @property imageId      尝试删除的图片 ID
     * @property errorMessage 失败原因描述
     */
    data class FAILED(
        val imageId: Long,
        val errorMessage: String
    ) : DeleteResult()
}
