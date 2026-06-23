/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 恢复图片用例
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.fileops.domain.usecase

import com.photocleaner.feature.fileops.domain.FileOperator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 从回收站恢复单张图片的用例。
 *
 * 接收回收站记录的主键 ID，调用 [FileOperator.restoreImage] 将文件移回
 * 原始位置并清理回收站记录。返回布尔值指示操作是否成功。
 *
 * @property fileOperator 文件操作接口
 *
 * @author PhotoCleaner
 */
@Singleton
class RestoreImageUseCase @Inject constructor(
    private val fileOperator: FileOperator
) {

    /**
     * 执行恢复操作。
     *
     * @param recycleItemId 回收站记录的主键 ID
     * @return true 恢复成功，false 恢复失败
     */
    suspend operator fun invoke(recycleItemId: Long): Boolean {
        return fileOperator.restoreImage(recycleItemId)
    }
}
