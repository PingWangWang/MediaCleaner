package com.photocleaner.feature.appupdate.domain.usecase

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.photocleaner.feature.appupdate.domain.repository.AppUpdateRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 安装 APK 用例
 *
 * 使用 FileProvider 创建安装 intent 并触发 APK 安装流程。
 * 如果缺少安装权限（Android 8+ 需要），返回 [Result.failure]。
 */
@Singleton
class InstallApkUseCase @Inject constructor(
    private val context: Context,
    private val repository: AppUpdateRepository
) {

    /**
     * 执行安装 APK 操作
     *
     * @param apkPath APK 文件的绝对路径
     * @return [Result]<[Boolean]> 成功返回 true，失败返回异常信息
     */
    suspend operator fun invoke(apkPath: String): Result<Boolean> {
        return try {
            // 检查是否有安装权限（Android 8.0+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    return Result.failure(
                        SecurityException("缺少安装未知应用的权限")
                    )
                }
            }

            val apkFile = File(apkPath)
            if (!apkFile.exists()) {
                return Result.failure(
                    IllegalArgumentException("APK 文件不存在: $apkPath")
                )
            }

            // 使用 FileProvider 获取 content URI
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)

            // 通知仓库安装已触发
            val result = repository.installApk(context, apkPath)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
