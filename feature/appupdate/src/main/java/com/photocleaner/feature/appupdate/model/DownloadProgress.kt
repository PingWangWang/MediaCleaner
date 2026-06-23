/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 下载进度模型
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.appupdate.model

/**
 * 下载进度密封类，表示下载过程中的各种状态
 *
 * DOWNLOADING: 下载中（不含进度数值的通用状态）
 * PROGRESS: 下载进度（包含已下载字节数和总字节数）
 * COMPLETED: 下载完成（包含 APK 文件路径）
 * FAILED: 下载失败（包含错误信息）
 */
sealed class DownloadProgress {

    /** 下载中（通用状态） */
    object DOWNLOADING : DownloadProgress()

    /** 下载进度 */
    data class PROGRESS(
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : DownloadProgress()

    /** 下载完成 */
    data class COMPLETED(
        val apkPath: String
    ) : DownloadProgress()

    /** 下载失败 */
    data class FAILED(
        val errorMessage: String
    ) : DownloadProgress()
}
