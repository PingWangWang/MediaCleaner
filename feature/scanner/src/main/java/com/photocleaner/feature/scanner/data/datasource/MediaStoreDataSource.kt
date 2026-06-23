package com.photocleaner.feature.scanner.data.datasource

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
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
 * MediaStore 数据源。
 *
 * 负责通过 [ContentResolver] 查询系统 MediaStore，
 * 将数据库游标映射为 [ImageItem] 领域模型，并以 Flow 形式分块返回。
 *
 * @author PhotoCleaner
 */
@Singleton
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        /** MediaStore 查询投影列 */
        private val PROJECTION = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.ORIENTATION
        )

        /** 各列的索引映射 */
        private const val COL_ID = 0
        private const val COL_DATA = 1
        private const val COL_NAME = 2
        private const val COL_SIZE = 3
        private const val COL_WIDTH = 4
        private const val COL_HEIGHT = 5
        private const val COL_MIME_TYPE = 6
        private const val COL_DATE_MODIFIED = 7
        private const val COL_ORIENTATION = 8

        /** MIME 类型过滤条件 */
        private val MIME_SELECTION = MediaConstants.IMAGE_MIME_TYPES.joinToString(" OR ") {
            "${MediaStore.Images.Media.MIME_TYPE} = ?"
        }
        private val MIME_SELECTION_ARGS = MediaConstants.IMAGE_MIME_TYPES

        /** 按修改时间降序排列 */
        private const val SORT_ORDER = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
    }

    private val contentResolver: ContentResolver
        get() = context.contentResolver

    /**
     * 查询 MediaStore 中所有符合条件的图片。
     *
     * 使用分页（每块 [AppConstants.SCAN_CHUNK_SIZE] 条）以减少内存占用，
     * 每块作为一个 List 发射。
     *
     * @return Flow 发射分块的图片列表
     */
    fun queryAllImages(): Flow<List<ImageItem>> = flow {
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        queryImages(uri, null)
    }.flowOn(Dispatchers.IO)

    /**
     * 查询指定时间之后修改过的图片（增量扫描）。
     *
     * @param since 时间戳（毫秒），只返回修改时间大于此值的图片
     * @return Flow 发射分块的图片列表
     */
    fun queryImagesModifiedSince(since: Long): Flow<List<ImageItem>> = flow {
        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val selection = "$MIME_SELECTION AND ${MediaStore.Images.Media.DATE_MODIFIED} > ?"
        val selectionArgs = MIME_SELECTION_ARGS + (since / 1000L).toString()
        queryImages(uri, selection, selectionArgs)
    }.flowOn(Dispatchers.IO)

    /**
     * 执行实际查询并分块发射结果。
     */
    private suspend fun FlowCollector<List<ImageItem>>.queryImages(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>? = null
    ) {
        val chunkSize = com.photocleaner.core.common.constant.AppConstants.SCAN_CHUNK_SIZE
        var offset = 0
        var hasMore = true

        while (hasMore) {
            val limit = "$offset,$chunkSize"
            var cursor: android.database.Cursor? = null
            try {
                cursor = contentResolver.query(
                    uri,
                    PROJECTION,
                    selection,
                    selectionArgs,
                    "$SORT_ORDER LIMIT $limit"
                )

                if (cursor == null || !cursor.moveToFirst()) {
                    hasMore = false
                    continue
                }

                val chunk = mutableListOf<ImageItem>()
                do {
                    val size = cursor.getLong(COL_SIZE)
                    // 过滤小于最小尺寸的图片
                    if (size < MediaConstants.MIN_IMAGE_SIZE) continue

                    val imageItem = mapCursorToImageItem(cursor)
                    chunk.add(imageItem)
                } while (cursor.moveToNext())

                if (chunk.isNotEmpty()) {
                    emit(chunk)
                }

                offset += chunkSize
            } catch (e: Exception) {
                // 查询失败则终止分页
                hasMore = false
            } finally {
                cursor?.close()
            }
        }
    }

    /**
     * 将当前游标行映射为 [ImageItem]。
     */
    private fun mapCursorToImageItem(cursor: android.database.Cursor): ImageItem {
        val id = cursor.getLong(COL_ID)
        val data = cursor.getString(COL_DATA)
        val name = cursor.getString(COL_NAME) ?: "unknown_$id"
        val size = cursor.getLong(COL_SIZE)
        val width = if (cursor.isNull(COL_WIDTH)) null else cursor.getInt(COL_WIDTH)
        val height = if (cursor.isNull(COL_HEIGHT)) null else cursor.getInt(COL_HEIGHT)
        val mimeType = cursor.getString(COL_MIME_TYPE)
        // DATE_MODIFIED 以秒为单位，转为毫秒
        val dateModified = cursor.getLong(COL_DATE_MODIFIED) * 1000L
        val orientation = if (cursor.isNull(COL_ORIENTATION)) 0 else cursor.getInt(COL_ORIENTATION)

        val uri = Uri.withAppendedPath(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id.toString()
        ).toString()

        return ImageItem(
            id = id,
            uri = uri,
            path = data,
            name = name,
            size = size,
            width = width,
            height = height,
            mimeType = mimeType,
            modifyTime = dateModified,
            orientation = orientation
        )
    }
}
