package com.photocleaner.core.common.extension

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import java.io.FileNotFoundException

/**
 * Context 扩展函数集合。
 *
 * 提供 URI 转 Bitmap、MediaStore 查询等与 Android 上下文相关的便捷操作。
 *
 * @author PhotoCleaner
 */

/**
 * 从指定 [uri] 中解码出 Bitmap。
 *
 * 自动处理 [MediaStore] 中的图片 URI，支持读取旋转信息。
 * 若解码失败或输入流不可用则返回 null。
 *
 * @param uri 图片的 [Uri] 地址
 * @return 解码后的 [Bitmap]，失败时返回 null
 */
fun Context.getBitmapFromUri(uri: Uri): Bitmap? {
    return try {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream)
        }
    } catch (e: FileNotFoundException) {
        null
    } catch (e: Exception) {
        null
    }
}

/**
 * 查询 MediaStore 内容提供器。
 *
 * 封装了 [contentResolver.query] 调用，自动处理可能的安全异常。
 * 通过 [projection] 指定返回列，[selection]/[selectionArgs] 构建查询条件，
 * [sortOrder] 指定排序方式。
 *
 * 使用示例：
 * ```
 * val cursor = context.queryMediaStore(
 *     projection = arrayOf(
 *         MediaStore.Images.Media._ID,
 *         MediaStore.Images.Media.DATA
 *     ),
 *     selection = "${MediaStore.Images.Media.SIZE} > ?",
 *     selectionArgs = arrayOf("1024"),
 *     sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
 * )
 * ```
 *
 * @param projection   返回的列名数组
 * @param selection    查询条件（WHERE 子句，不含 WHERE 关键字）
 * @param selectionArgs 查询条件占位符参数
 * @param sortOrder    排序方式
 * @return [Cursor] 对象，查询失败时返回 null
 */
fun Context.queryMediaStore(
    projection: Array<String>,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null
): Cursor? {
    return try {
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )
    } catch (e: SecurityException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    } catch (e: Exception) {
        null
    }
}
