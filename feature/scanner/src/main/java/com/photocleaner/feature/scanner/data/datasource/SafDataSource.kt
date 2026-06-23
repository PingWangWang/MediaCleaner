package com.photocleaner.feature.scanner.data.datasource

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.photocleaner.core.common.constant.MediaConstants
import com.photocleaner.core.common.model.ImageItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SAF（Storage Access Framework）数据源。
 *
 * 通过 SAF 授予的目录 [Uri] 递归扫描其中的图片文件，
 * 并映射为 [ImageItem] 领域模型。
 *
 * @author PhotoCleaner
 */
@Singleton
class SafDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        /** DocumentsContract 查询投影列 */
        private val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        private const val COL_DOCUMENT_ID = 0
        private const val COL_NAME = 1
        private const val COL_SIZE = 2
        private const val COL_LAST_MODIFIED = 3
        private const val COL_MIME_TYPE = 4
    }

    /**
     * 扫描 SAF 授权目录下的所有图片文件。
     *
     * 递归遍历子目录，对每个匹配 MIME 类型且大小达标的文件
     * 发射一个 [ImageItem]。
     *
     * @param uri SAF 授予的目录 Uri
     * @return Flow 逐个发射扫描到的图片
     */
    fun scanDirectory(uri: Uri): Flow<ImageItem> = flow {
        scanDirectoryRecursive(uri)
    }.flowOn(Dispatchers.IO)

    /**
     * 递归扫描目录。
     */
    private suspend fun FlowCollector<ImageItem>.scanDirectoryRecursive(directoryUri: Uri) {
        val acceptedMimeTypes = MediaConstants.IMAGE_MIME_TYPES.toSet()

        var cursor: android.database.Cursor? = null
        try {
            cursor = context.contentResolver.query(
                directoryUri,
                PROJECTION,
                null,
                null,
                null
            )

            if (cursor == null) return

            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(COL_MIME_TYPE) ?: continue
                val documentId = cursor.getString(COL_DOCUMENT_ID) ?: continue

                if (DocumentsContract.Document.MIME_TYPE_DIR == mimeType) {
                    // 递归扫描子目录
                    val subDirUri = DocumentsContract.buildDocumentUriUsingTree(
                        directoryUri,
                        documentId
                    )
                    scanDirectoryRecursive(subDirUri)
                } else if (mimeType in acceptedMimeTypes) {
                    val size = cursor.getLong(COL_SIZE)

                    // 过滤小于最小尺寸的图片
                    if (size < MediaConstants.MIN_IMAGE_SIZE) continue

                    val name = cursor.getString(COL_NAME) ?: "unknown_$documentId"
                    val lastModified = cursor.getLong(COL_LAST_MODIFIED)

                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(
                        directoryUri,
                        documentId
                    )

                    // 尝试通过 OpenableColumns 获取更精确的信息
                    var refinedName: String
                    var refinedSize: Long
                    var refinedCursor: android.database.Cursor? = null
                    try {
                        refinedCursor = context.contentResolver.query(
                            fileUri,
                            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                            null,
                            null,
                            null
                        )
                        if (refinedCursor != null && refinedCursor.moveToFirst()) {
                            refinedName = refinedCursor.getString(0) ?: name
                            refinedSize = if (refinedCursor.isNull(1)) size else refinedCursor.getLong(1)
                        } else {
                            refinedName = name
                            refinedSize = size
                        }
                    } catch (_: Exception) {
                        refinedName = name
                        refinedSize = size
                    } finally {
                        refinedCursor?.close()
                    }

                    val imageItem = ImageItem(
                        id = documentId.hashCode().toLong(),
                        uri = fileUri.toString(),
                        path = fileUri.toString(),
                        name = refinedName,
                        size = refinedSize,
                        mimeType = mimeType,
                        modifyTime = if (lastModified > 0) lastModified else System.currentTimeMillis()
                    )

                    emit(imageItem)
                }
            }
        } catch (_: Exception) {
            // 忽略无法访问的目录或文件
        } finally {
            cursor?.close()
        }
    }
}
