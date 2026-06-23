/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 下载 APK 用例
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.appupdate.domain.usecase

import com.photocleaner.feature.appupdate.domain.repository.AppUpdateRepository
import com.photocleaner.feature.appupdate.model.DownloadProgress
import com.photocleaner.feature.appupdate.model.UpdateInfo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 下载 APK 用例
 *
 * 根据 [UpdateInfo] 中的下载地址和 MD5 值下载 APK 文件，
 * 返回下载进度的 Flow。
 */
@Singleton
class DownloadApkUseCase @Inject constructor(
    private val repository: AppUpdateRepository
) {

    /**
     * 执行下载 APK 操作
     *
     * @param updateInfo 更新信息，包含下载地址和 MD5 校验值
     * @return [Flow]<[DownloadProgress]> 下载进度流
     */
    operator fun invoke(updateInfo: UpdateInfo): Flow<DownloadProgress> {
        return repository.downloadApk(
            url = updateInfo.downloadUrl,
            md5 = updateInfo.md5
        )
    }
}
