/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 检查更新用例
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.appupdate.domain.usecase

import com.photocleaner.feature.appupdate.domain.repository.AppUpdateRepository
import com.photocleaner.feature.appupdate.model.UpdateInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 检查更新用例
 *
 * 封装检查更新的业务逻辑，返回 [Result]<[UpdateInfo]>，
 * 成功时包含更新信息，失败时包含异常信息。
 */
@Singleton
class CheckUpdateUseCase @Inject constructor(
    private val repository: AppUpdateRepository
) {

    /**
     * 执行检查更新操作
     *
     * @param currentVersionCode 当前应用的版本号
     * @return [Result]<[UpdateInfo]> 包含更新信息或异常
     */
    suspend operator fun invoke(currentVersionCode: Int): Result<UpdateInfo> {
        return try {
            val updateInfo = repository.checkUpdate(currentVersionCode)
            if (updateInfo.updateType == com.photocleaner.feature.appupdate.model.UpdateType.NO_UPDATE) {
                Result.success(updateInfo)
            } else {
                Result.success(updateInfo)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
