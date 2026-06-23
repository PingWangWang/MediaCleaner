/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 应用升级仓库实现
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.appupdate.data.repository

import android.content.Context
import com.photocleaner.feature.appupdate.data.datasource.ApkDownloader
import com.photocleaner.feature.appupdate.data.datasource.UpdateApiDataSource
import com.photocleaner.feature.appupdate.domain.repository.AppUpdateRepository
import com.photocleaner.feature.appupdate.model.DownloadProgress
import com.photocleaner.feature.appupdate.model.UpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用更新仓库实现
 *
 * 桥接数据源层（[UpdateApiDataSource] 和 [ApkDownloader]）
 * 与领域层，所有耗时操作均在 [Dispatchers.IO] 上执行。
 */
@Singleton
class AppUpdateRepositoryImpl @Inject constructor(
    private val apiDataSource: UpdateApiDataSource,
    private val apkDownloader: ApkDownloader
) : AppUpdateRepository {

    override suspend fun checkUpdate(currentVersionCode: Int): UpdateInfo {
        return withContext(Dispatchers.IO) {
            apiDataSource.fetchUpdateInfo(currentVersionCode)
        }
    }

    override fun downloadApk(url: String, md5: String): Flow<DownloadProgress> {
        return apkDownloader.download(url, md5)
    }

    override suspend fun installApk(context: Context, apkPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 在实际安装流程中，InstallApkUseCase 已通过 Intent 触发安装，
                // 这里返回 true 表示安装流程已成功启动
                true
            } catch (e: Exception) {
                false
            }
        }
    }
}
