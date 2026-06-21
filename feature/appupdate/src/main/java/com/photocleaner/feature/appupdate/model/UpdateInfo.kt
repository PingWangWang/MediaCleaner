package com.photocleaner.feature.appupdate.model

/**
 * 应用更新信息数据类
 *
 * @property latestVersion 最新版本名称，如 "1.0.1"
 * @property latestVersionCode 最新版本号
 * @property updateType 更新类型（强制/可选/无更新）
 * @property downloadUrl APK 下载地址
 * @property releaseNotes 版本发布说明
 * @property md5 APK 文件的 MD5 校验值
 * @property fileSize APK 文件大小（字节）
 * @property minSupportedVersion 最低支持的版本（可选），用于兼容性检查
 */
data class UpdateInfo(
    val latestVersion: String,
    val latestVersionCode: Int,
    val updateType: UpdateType,
    val downloadUrl: String,
    val releaseNotes: String,
    val md5: String,
    val fileSize: Long,
    val minSupportedVersion: String? = null
)
