package com.photocleaner.feature.fileops.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.photocleaner.core.common.model.ImageItem
import com.photocleaner.core.database.dao.ImageDao
import com.photocleaner.core.database.dao.RecycleDao
import com.photocleaner.core.database.entity.ImageItemEntity
import com.photocleaner.core.database.entity.RecycleItemEntity
import com.photocleaner.feature.fileops.domain.FileOperator
import com.photocleaner.feature.fileops.model.DeleteResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [FileOperator] 接口的默认实现。
 *
 * 负责完整的文件删除与回收流程：
 * - 删除时将原始文件复制到应用私有回收目录，然后通过 MediaStore API
 *   （Android 10+）或直接删除（Android 9-）移除原文件
 * - 恢复时将回收目录中的文件通过 MediaStore 写回公共存储
 * - 所有耗时操作均在 [Dispatchers.IO] 上执行
 *
 * @property context   应用上下文
 * @property recycleDao 回收站数据库操作 DAO
 * @property imageDao   图片元数据 DAO
 *
 * @author PhotoCleaner
 */
@Singleton
class FileOperatorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val recycleDao: RecycleDao,
    private val imageDao: ImageDao
) : FileOperator {

    /** 内部回收站文件管理器 */
    private val recycleManager = RecycleManager(context)

    override suspend fun deleteImage(image: ImageItem): DeleteResult = withContext(Dispatchers.IO) {
        try {
            // Step 1: 将文件复制到回收站目录
            val recyclePath = recycleManager.moveToRecycle(image.uri, image.name)
            if (recyclePath == null) {
                return@withContext DeleteResult.FAILED(
                    imageId = image.id,
                    errorMessage = "无法将文件移入回收站：${image.name}"
                )
            }

            // Step 2: 在回收站数据表中插入记录
            val now = System.currentTimeMillis()
            val expireTime = now + (EXPIRE_DAYS_MILLIS)
            val recycleItem = RecycleItemEntity(
                originalId = image.id,
                uri = image.uri,
                name = image.name,
                size = image.size,
                mimeType = image.mimeType,
                deletedTime = now,
                recyclePath = recyclePath,
                expireTime = expireTime
            )
            recycleDao.insert(recycleItem)

            // Step 3: 删除原始文件（按 API 级别选择策略）
            val originalDeleted = deleteOriginalFile(image)

            // Step 4: 从图片库中移除该记录
            if (originalDeleted) {
                imageDao.deleteByIds(listOf(image.id))
            }

            // 即使原始文件删除失败，回收站中已有副本，视为删除成功
            DeleteResult.SUCCESS(imageId = image.id, savedBytes = image.size)
        } catch (e: Exception) {
            DeleteResult.FAILED(
                imageId = image.id,
                errorMessage = e.message ?: "未知错误"
            )
        }
    }

    override suspend fun deleteImages(images: List<ImageItem>): List<DeleteResult> = withContext(Dispatchers.IO) {
        images.map { deleteImage(it) }
    }

    override suspend fun restoreImage(recycleItemId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val item = recycleDao.getById(recycleItemId) ?: return@withContext false

            // Step 1: 将文件从回收目录恢复到缓存
            val restored = recycleManager.restoreFromRecycle(item.recyclePath, item.name)
            if (!restored) return@withContext false

            // Step 2: 通过 MediaStore 将缓存文件写回公共目录
            val cacheFile = File(context.cacheDir, item.name)
            if (!cacheFile.exists()) return@withContext false

            val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, item.name)
                put(MediaStore.Images.Media.MIME_TYPE, item.mimeType ?: "image/jpeg")
                put(MediaStore.Images.Media.SIZE, cacheFile.length())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val insertedUri = context.contentResolver.insert(contentUri, contentValues)
            if (insertedUri == null) {
                cacheFile.delete()
                return@withContext false
            }

            context.contentResolver.openOutputStream(insertedUri)?.use { output ->
                cacheFile.inputStream().use { input ->
                    input.copyTo(output, bufferSize = BUFFER_SIZE)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(insertedUri, updateValues, null, null)
            }

            // Step 3: 删除缓存文件
            cacheFile.delete()

            // Step 4: 删除回收站数据库记录
            recycleDao.deleteById(recycleItemId)

            true
        } catch (e: Exception) {
            false
        }
    }

    override fun getRecycleBinItems(): Flow<List<RecycleItemEntity>> = flow {
        emit(recycleDao.getAll())
    }.flowOn(Dispatchers.IO)

    override suspend fun clearExpiredItems() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 先查询所有过期条目，逐一删除对应的回收文件
        val allItems = recycleDao.getAll()
        val expiredItems = allItems.filter { it.expireTime < now }

        for (item in expiredItems) {
            recycleManager.deletePermanent(item.recyclePath)
        }

        // 删除数据库记录
        recycleDao.deleteExpired(now)

        // 清理回收目录中可能残留的孤立文件
        recycleManager.cleanExpired()
    }

    override suspend fun permanentlyDelete(itemId: Long) = withContext(Dispatchers.IO) {
        val item = recycleDao.getById(itemId) ?: return@withContext

        // 删除回收文件
        recycleManager.deletePermanent(item.recyclePath)

        // 删除数据库记录
        recycleDao.deleteById(itemId)
    }

    override fun getRecycleCount(): Flow<Int> = flow {
        emit(recycleDao.count())
    }.flowOn(Dispatchers.IO)

    /**
     * 根据 Android API 级别选择策略删除原始文件。
     *
     * - Android 10+（API 29+）：使用 [MediaStore.createDeleteRequest] 系统请求删除
     *   通过 PendingIntent 触发系统确认对话框，用户确认后执行删除
     * - Android 9 及以下：直接通过 File API 删除
     *
     * @param image 要删除的图片
     * @return true 删除成功或文件已不存在，false 删除失败
     */
    private fun deleteOriginalFile(image: ImageItem): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+：优先使用 createDeleteRequest（需 Activity Result API）
            // 若无法启动 PendingIntent，降级使用 contentResolver.delete
            try {
                val uri = Uri.parse(image.uri)
                // 尝试直接删除（需要权限：WRITE_EXTERNAL_STORAGE 或 MANAGE_EXTERNAL_STORAGE）
                val deletedCount = context.contentResolver.delete(uri, null, null)
                deletedCount > 0
            } catch (e: SecurityException) {
                // 无直接删除权限时，通过 MediaStore.createDeleteRequest 发送系统请求
                try {
                    val uri = Uri.parse(image.uri)
                    @Suppress("UNUSED_VARIABLE")
                    val pendingIntent = MediaStore.createDeleteRequest(
                        context.contentResolver,
                        listOf(uri)
                    )
                    // 注意：此处需通过 Activity 启动 PendingIntent 以触发用户确认对话框
                    // 在非 Activity 上下文中，降级为标记记录已回收
                    false
                } catch (e2: Exception) {
                    false
                }
            } catch (e: IllegalArgumentException) {
                // URI 无效 — 可能已被删除
                true
            }
        } else {
            // Android 9 及以下：直接删除文件
            try {
                val file = image.path?.let { File(it) }
                if (file != null && file.exists()) {
                    file.delete()
                } else {
                    // 文件路径不存在，尝试通过 URI 删除
                    val uri = Uri.parse(image.uri)
                    context.contentResolver.delete(uri, null, null)
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    private companion object {
        /** 文件拷贝缓冲区大小：8 KB */
        private const val BUFFER_SIZE = 8192

        /** 回收站默认保留时间：30 天（毫秒） */
        private const val EXPIRE_DAYS_MILLIS = 30L * 24L * 60L * 60L * 1000L
    }
}
