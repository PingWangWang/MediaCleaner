/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 应用升级仓库接口
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.appupdate.domain.repository

import android.content.Context
import com.photocleaner.feature.appupdate.model.DownloadProgress
import com.photocleaner.feature.appupdate.model.UpdateInfo
import kotlinx.coroutines.flow.Flow

/**
 * 应用更新仓库接口
 *
 * 定义检查更新、下载 APK、安装 APK 的核心操作，
 * 具体实现由 data 层提供。
 */
interface AppUpdateRepository {

    /**
     * 检查是否有可用更新
     *
     * @param currentVersionCode 当前应用的版本号
     * @return [UpdateInfo] 包含更新信息
     */
    suspend fun checkUpdate(currentVersionCode: Int): UpdateInfo

    /**
     * 下载 APK 文件
     *
     * @param url APK 下载地址
     * @param md5 APK 文件的 MD5 校验值
     * @return [Flow]<[DownloadProgress]> 下载进度流
     */
    fun downloadApk(url: String, md5: String): Flow<DownloadProgress>

    /**
     * 安装 APK 文件
     *
     * @param context Android 上下文
     * @param apkPath APK 文件路径
     * @return 安装是否成功
     */
    suspend fun installApk(context: Context, apkPath: String): Boolean
}
