package com.photocleaner.app.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 权限辅助工具类。
 *
 * 集中管理存储权限的检查、请求及 rationale 判断，
 * 根据 API 级别自动选择 READ_MEDIA_IMAGES (API 33+) 或 READ_EXTERNAL_STORAGE (API <33)。
 *
 * @author PhotoCleaner
 */
@Singleton
class PermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        /** 存储权限请求码 */
        const val REQUEST_CODE_STORAGE = 100
    }

    /**
     * 检查应用是否已获得所需的存储权限。
     *
     * @return true 如果权限已授予，否则 false
     */
    fun hasRequiredPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * 请求存储权限。
     *
     * @param activity  当前 Activity
     * @param requestCode 请求码，默认使用 [REQUEST_CODE_STORAGE]
     */
    fun requestPermission(activity: Activity, requestCode: Int = REQUEST_CODE_STORAGE) {
        val permission = getRequiredPermission()
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

    /**
     * 判断是否需要显示权限 rationale（用户此前拒绝过权限请求）。
     *
     * @param activity 当前 Activity
     * @return true 如果应该显示 rationale
     */
    fun shouldShowPermissionRationale(activity: Activity): Boolean {
        val permission = getRequiredPermission()
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * 根据 API 级别返回当前所需的权限字符串。
     */
    private fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}
