package com.photocleaner.feature.fileops.data

import android.content.Context
import android.net.Uri
import com.photocleaner.core.common.constant.AppConstants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 回收站文件管理器。
 *
 * 负责回收目录中实际文件的物理操作：将图片移入回收站、从回收站恢复、
 * 清理过期文件以及永久删除。所有文件存储于应用私有目录下的 `recycle/`
 * 子目录中，避免触发 MediaStore 或 SAF 权限问题。
 *
 * @property context 应用上下文（ApplicationContext），用于获取私有文件目录
 *
 * @author PhotoCleaner
 */
@Singleton
class RecycleManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        /** 回收站文件存放的子目录名称，位于应用私有文件目录下 */
        private const val RECYCLE_DIR_NAME = "recycle"

        /** 回收站文件默认过期天数 */
        private val EXPIRE_DAYS: Long = AppConstants.RECYCLE_AUTO_CLEAR_DAYS.toLong()
    }

    /** 回收站目录的 File 句柄，按需惰性创建 */
    private val recycleDir: File by lazy {
        val dir = File(context.filesDir, RECYCLE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    /**
     * 将源文件移动至回收站目录。
     *
     * 执行步骤：
     * 1. 以 [fileName] 在回收目录中创建目标文件（若重名则附加时间戳）
     * 2. 从 [sourceUri] 读取输入流并写入目标文件
     * 3. 返回回收目录中的绝对路径；若失败返回 null
     *
     * @param sourceUri 原始图片的 Content URI 字符串
     * @param fileName  原始文件名，用于回收目录中的命名
     * @return 回收站中的绝对路径字符串，失败时返回 null
     */
    fun moveToRecycle(sourceUri: String, fileName: String): String? {
        return try {
            val targetFile = resolveTargetFile(fileName)
            val uri = Uri.parse(sourceUri)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output, bufferSize = DEFAULT_BUFFER_SIZE)
                }
            } ?: return null

            // 确认写入成功
            if (!targetFile.exists() || targetFile.length() == 0L) {
                targetFile.delete()
                return null
            }

            targetFile.absolutePath
        } catch (e: IOException) {
            return null
        } catch (e: SecurityException) {
            return null
        }
    }

    /**
     * 从回收站恢复文件到原始位置。
     *
     * 将 [recyclePath] 对应的文件复制到外部存储中由 [originalName] 命名的位置。
     * 注意：Android 10+ 上无法直接写入任意路径，此方法返回布尔值表示文件是否
     * 已准备好被外部调用方（如 MediaStore）处理。
     *
     * 实际的文件"恢复"会由 [FileOperatorImpl] 配合 MediaStore API 完成，
     * 此处仅将文件从回收目录移回一个临时位置供后续处理。
     *
     * @param recyclePath  回收站中文件的绝对路径
     * @param originalName 原始文件名
     * @return true 恢复成功，false 失败
     */
    fun restoreFromRecycle(recyclePath: String, originalName: String): Boolean {
        return try {
            val recycleFile = File(recyclePath)
            if (!recycleFile.exists()) return false

            // 先将文件从回收目录复制到缓存目录，命名为原始名称
            // 后续 restoreImage 会将此文件通过 MediaStore 写回公共目录
            val cacheFile = File(context.cacheDir, originalName)
            FileInputStream(recycleFile).use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output, bufferSize = DEFAULT_BUFFER_SIZE)
                }
            }

            // 确认写入成功
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                cacheFile.delete()
                return false
            }

            // 删除回收目录中的源文件
            recycleFile.delete()

            true
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * 清理所有已过期的回收站文件。
     *
     * 遍历回收目录，删除所有修改时间早于当前时间减去过期天数的文件。
     * 此方法由 [FileOperatorImpl.clearExpiredItems] 在清除数据库记录后调用，
     * 用于清理可能残留的孤立文件。
     */
    suspend fun cleanExpired() = withContext(Dispatchers.IO) {
        val expireThreshold = System.currentTimeMillis() - (EXPIRE_DAYS * MILLIS_PER_DAY)
        val files = recycleDir.listFiles() ?: return@withContext
        for (file in files) {
            if (file.isFile && file.lastModified() < expireThreshold) {
                file.delete()
            }
        }
    }

    /**
     * 永久删除回收站中的指定文件。
     *
     * @param recyclePath 回收站中文件的绝对路径
     * @return true 文件已被删除或不存在，false 删除失败
     */
    fun deletePermanent(recyclePath: String): Boolean {
        return try {
            val file = File(recyclePath)
            if (!file.exists()) return true // 已不存在视为成功
            file.delete()
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * 解析目标文件，若重名则自动附加时间戳以避免覆盖。
     */
    private fun resolveTargetFile(fileName: String): File {
        val target = File(recycleDir, fileName)
        return if (target.exists()) {
            val dotIndex = fileName.lastIndexOf('.')
            val baseName = if (dotIndex > 0) fileName.substring(0, dotIndex) else fileName
            val extension = if (dotIndex > 0) fileName.substring(dotIndex) else ""
            val timestampSuffix = "_${System.currentTimeMillis()}"
            File(recycleDir, "${baseName}${timestampSuffix}${extension}")
        } else {
            target
        }
    }

    private companion object {
        /** 文件拷贝缓冲区大小：8 KB */
        private const val DEFAULT_BUFFER_SIZE = 8192

        /** 一天的毫秒数 */
        private const val MILLIS_PER_DAY = 86_400_000L
    }
}
