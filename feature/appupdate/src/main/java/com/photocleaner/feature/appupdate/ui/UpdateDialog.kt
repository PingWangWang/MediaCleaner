package com.photocleaner.feature.appupdate.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.photocleaner.feature.appupdate.model.DownloadProgress
import com.photocleaner.feature.appupdate.model.UpdateInfo
import com.photocleaner.feature.appupdate.model.UpdateType

/**
 * 更新通知对话框
 *
 * Material3 风格的更新弹窗，展示版本发布说明、
 * 下载进度条和操作按钮。
 *
 * @param updateInfo 更新信息
 * @param onDismiss 关闭对话框回调
 * @param onDownload 开始下载回调
 * @param downloadProgress 当前下载进度（为 null 时尚未开始下载）
 */
@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    downloadProgress: DownloadProgress?
) {
    // 是否正在下载
    var isDownloading by remember { mutableStateOf(false) }
    // 下载是否已完成
    var isDownloaded by remember { mutableStateOf(false) }

    // 同步外部 downloadProgress 到本地状态
    when (downloadProgress) {
        is DownloadProgress.PROGRESS -> {
            isDownloading = true
        }
        is DownloadProgress.COMPLETED -> {
            isDownloading = false
            isDownloaded = true
        }
        is DownloadProgress.FAILED -> {
            isDownloading = false
            isDownloaded = false
        }
        else -> { /* DOWNLOADING 或 null，保持现有状态 */ }
    }

    val isForcedUpdate = updateInfo.updateType == UpdateType.FORCED

    AlertDialog(
        onDismissRequest = {
            if (!isForcedUpdate && !isDownloading) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = "发现新版本",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 版本信息
                Text(
                    text = "版本 ${updateInfo.latestVersion}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 文件大小
                Text(
                    text = "文件大小: ${formatFileSize(updateInfo.fileSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 发布说明
                Text(
                    text = "更新内容:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = updateInfo.releaseNotes.ifBlank { "暂无更新说明" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 下载进度指示器
                if (isDownloading || downloadProgress != null) {
                    when (val progress = downloadProgress) {
                        is DownloadProgress.PROGRESS -> {
                            val progressValue = if (progress.totalBytes > 0) {
                                progress.bytesDownloaded.toFloat() / progress.totalBytes.toFloat()
                            } else {
                                0f
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    progress = { progressValue },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "${formatFileSize(progress.bytesDownloaded)} / ${formatFileSize(progress.totalBytes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is DownloadProgress.DOWNLOADING -> {
                            // 无具体进度的下载中状态
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "正在下载...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is DownloadProgress.FAILED -> {
                            Text(
                                text = "下载失败: ${progress.errorMessage}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is DownloadProgress.COMPLETED -> {
                            Text(
                                text = "下载完成 ✓",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        null -> { /* 尚未开始下载，不显示进度 */ }
                    }
                }
            }
        },
        confirmButton = {
            when {
                // 下载完成后显示"安装"按钮
                isDownloaded -> {
                    Button(onClick = onDismiss) {
                        Text("安装")
                    }
                }
                // 正在下载中显示"取消"或不可操作状态
                isDownloading -> {
                    TextButton(onClick = { /* 可在后续版本支持取消下载 */ }) {
                        Text(
                            "下载中...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // 尚未开始下载，显示"立即更新"
                else -> {
                    Button(onClick = {
                        isDownloading = true
                        onDownload()
                    }) {
                        Text("立即更新")
                    }
                }
            }
        },
        dismissButton = {
            // 强制更新不显示取消按钮；下载中也不可关闭
            if (!isForcedUpdate && !isDownloading && !isDownloaded) {
                TextButton(onClick = onDismiss) {
                    Text("稍后再说")
                }
            }
        }
    )
}

/**
 * 格式化文件大小为可读字符串
 *
 * @param bytes 字节数
 * @return 格式化后的字符串（如 "5.0 MB"）
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
