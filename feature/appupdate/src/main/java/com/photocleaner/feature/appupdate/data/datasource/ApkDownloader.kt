/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * APK 文件下载器，支持断点续传
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.appupdate.data.datasource

import android.content.Context
import com.photocleaner.feature.appupdate.model.DownloadProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * APK 下载器
 *
 * 使用 OkHttp 实现流式下载，支持断点续传、
 * MD5 校验和进度通知。下载文件保存在应用
 * 私有目录的 'updates/' 文件夹中。
 */
@Singleton
class ApkDownloader @Inject constructor(
    private val context: Context
) {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /** 下载目录：应用私有目录下的 updates/ */
    private val downloadDir: File by lazy {
        val dir = File(context.filesDir, "updates")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    /**
     * 下载 APK 文件，返回下载进度流
     *
     * 支持断点续传：如果本地已存在同名文件且未完成，
     * 会从已下载的位置继续下载。
     *
     * @param url APK 下载地址
     * @param md5 APK 文件的 MD5 校验值（用于下载完成后的完整性校验）
     * @return [Flow]<[DownloadProgress]> 下载进度流
     */
    fun download(url: String, md5: String): Flow<DownloadProgress> = flow {
        val fileName = url.substringAfterLast("/", "update.apk")
        val tempFile = File(downloadDir, "${fileName}.tmp")
        val finalFile = File(downloadDir, fileName)

        // 如果最终文件已存在且 MD5 匹配，直接返回完成
        if (finalFile.exists() && verifyMd5(finalFile, md5)) {
            emit(DownloadProgress.COMPLETED(apkPath = finalFile.absolutePath))
            return@flow
        }

        // 获取已下载字节数（用于断点续传）
        val downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L

        emit(DownloadProgress.DOWNLOADING)

        val request = Request.Builder()
            .url(url)
            .apply {
                if (downloadedBytes > 0) {
                    addHeader("Range", "bytes=$downloadedBytes-")
                }
            }
            .build()

        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful && response.code != 206) {
            emit(
                DownloadProgress.FAILED(
                    errorMessage = "下载失败，HTTP ${response.code}"
                )
            )
            return@flow
        }

        val responseBody = response.body ?: run {
            emit(DownloadProgress.FAILED(errorMessage = "响应体为空"))
            return@flow
        }

        val totalBytes = downloadedBytes + (responseBody.contentLength().coerceAtLeast(0))

        // 使用 RandomAccessFile 支持断点续写
        val outputStream = if (downloadedBytes > 0) {
            RandomAccessFile(tempFile, "rw").apply {
                seek(downloadedBytes)
            }
        } else {
            RandomAccessFile(tempFile, "rw")
        }

        try {
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var bytesSoFar = downloadedBytes
            var lastEmitTime = System.currentTimeMillis()
            val inputStream = responseBody.byteStream()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesSoFar += bytesRead

                val now = System.currentTimeMillis()
                // 每秒最多发射 4 次进度，避免 UI 过度刷新
                if (now - lastEmitTime >= 250) {
                    emit(
                        DownloadProgress.PROGRESS(
                            bytesDownloaded = bytesSoFar,
                            totalBytes = totalBytes
                        )
                    )
                    lastEmitTime = now
                }
            }

            outputStream.close()

            // 下载完成，重命名临时文件
            if (tempFile.renameTo(finalFile)) {
                // MD5 校验
                if (verifyMd5(finalFile, md5)) {
                    emit(
                        DownloadProgress.COMPLETED(apkPath = finalFile.absolutePath)
                    )
                } else {
                    // MD5 不匹配，删除文件并报告失败
                    finalFile.delete()
                    emit(
                        DownloadProgress.FAILED(
                            errorMessage = "MD5 校验失败，文件可能已损坏"
                        )
                    )
                }
            } else {
                emit(
                    DownloadProgress.FAILED(
                        errorMessage = "文件重命名失败"
                    )
                )
            }
        } catch (e: Exception) {
            outputStream.close()
            emit(
                DownloadProgress.FAILED(
                    errorMessage = "下载异常: ${e.message ?: "未知错误"}"
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 验证文件的 MD5 值
     *
     * @param file 待校验的文件
     * @param expectedMd5 期望的 MD5 值（32 位十六进制字符串）
     * @return 校验是否通过
     */
    private fun verifyMd5(file: File, expectedMd5: String): Boolean {
        if (expectedMd5.isBlank()) {
            // 如果没有提供 MD5，跳过校验
            return true
        }

        return try {
            val digest = MessageDigest.getInstance("MD5")
            file.inputStream().use { inputStream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val md5Hex = digest.digest().joinToString("") { "%02x".format(it) }
            md5Hex.equals(expectedMd5, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }
}
