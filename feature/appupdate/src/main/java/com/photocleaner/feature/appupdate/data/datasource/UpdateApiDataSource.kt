package com.photocleaner.feature.appupdate.data.datasource

import com.photocleaner.feature.appupdate.model.UpdateInfo
import com.photocleaner.feature.appupdate.model.UpdateType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 更新 API 数据源
 *
 * 负责通过网络请求获取服务器上的最新版本信息，
 * 解析 JSON 响应并转换为 [UpdateInfo] 领域模型。
 * 配置项可通过构造函数参数或常量覆盖。
 */
@Singleton
class UpdateApiDataSource @Inject constructor() {

    /** 更新检查 URL（可通过配置文件或 BuildConfig 覆盖） */
    private var updateCheckUrl: String = "https://api.photocleaner.app/update/check"

    /** 网络请求超时时间（秒） */
    private val connectTimeout = 15L
    private val readTimeout = 15L

    /** OkHttp 客户端（懒加载，可注入自定义实例） */
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /**
     * 设置更新检查 URL（用于测试或动态配置）
     */
    fun setUpdateCheckUrl(url: String) {
        updateCheckUrl = url
    }

    /**
     * 从服务器获取更新信息
     *
     * @param currentVersionCode 当前应用的版本号
     * @return [UpdateInfo] 如果服务器返回了更新信息则返回对应数据，
     *         如果网络异常或没有可用更新则返回 [UpdateType.NO_UPDATE]
     */
    suspend fun fetchUpdateInfo(currentVersionCode: Int): UpdateInfo {
        return try {
            val request = Request.Builder()
                .url("$updateCheckUrl?currentVersionCode=$currentVersionCode")
                .get()
                .addHeader("Accept", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                return createNoUpdateInfo()
            }

            val bodyString = response.body?.string()
            if (bodyString.isNullOrBlank()) {
                return createNoUpdateInfo()
            }

            parseUpdateResponse(bodyString, currentVersionCode)
        } catch (e: Exception) {
            // 网络错误或解析异常时返回无更新
            createNoUpdateInfo()
        }
    }

    /**
     * 解析服务端 JSON 响应
     *
     * 期望 JSON 格式：
     * {
     *   "hasUpdate": true,
     *   "latestVersion": "1.0.1",
     *   "latestVersionCode": 2,
     *   "updateType": "FORCED",
     *   "downloadUrl": "https://.../app.apk",
     *   "releaseNotes": "新版本修复了若干问题",
     *   "md5": "d41d8cd98f00b204e9800998ecf8427e",
     *   "fileSize": 5242880,
     *   "minSupportedVersion": "1.0.0"
     * }
     */
    private fun parseUpdateResponse(
        jsonString: String,
        currentVersionCode: Int
    ): UpdateInfo {
        val json = JSONObject(jsonString)

        val hasUpdate = json.optBoolean("hasUpdate", false)
        if (!hasUpdate) {
            return createNoUpdateInfo()
        }

        val latestVersionCode = json.optInt("latestVersionCode", 0)
        // 如果服务器版本号不大于当前版本，视为无更新
        if (latestVersionCode <= currentVersionCode) {
            return createNoUpdateInfo()
        }

        val updateTypeStr = json.optString("updateType", "OPTIONAL")
        val updateType = when (updateTypeStr.uppercase()) {
            "FORCED" -> UpdateType.FORCED
            "OPTIONAL" -> UpdateType.OPTIONAL
            else -> UpdateType.OPTIONAL
        }

        return UpdateInfo(
            latestVersion = json.optString("latestVersion", ""),
            latestVersionCode = latestVersionCode,
            updateType = updateType,
            downloadUrl = json.optString("downloadUrl", ""),
            releaseNotes = json.optString("releaseNotes", ""),
            md5 = json.optString("md5", ""),
            fileSize = json.optLong("fileSize", 0L),
            minSupportedVersion = json.optString("minSupportedVersion", "").ifEmpty { null }
        )
    }

    /**
     * 创建表示"无更新"的 [UpdateInfo]
     */
    private fun createNoUpdateInfo(): UpdateInfo {
        return UpdateInfo(
            latestVersion = "",
            latestVersionCode = 0,
            updateType = UpdateType.NO_UPDATE,
            downloadUrl = "",
            releaseNotes = "",
            md5 = "",
            fileSize = 0L,
            minSupportedVersion = null
        )
    }
}
