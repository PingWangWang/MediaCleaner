package com.photocleaner.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a recycled (soft-deleted) image in the [recycle_item] table.
 *
 * When the user chooses to delete duplicate images, the originals are moved to a
 * recycle directory rather than permanently removed. This table tracks those items
 * so they can be restored or permanently purged after [expireTime].
 *
 * @property id Auto-generated primary key.
 * @property originalId The original [ImageItemEntity.id] that was recycled.
 * @property uri Content URI of the original image.
 * @property name Display name of the recycled file.
 * @property size File size in bytes.
 * @property mimeType MIME type of the image, e.g. image/jpeg.
 * @property deletedTime Timestamp when the item was moved to recycle (millis since epoch).
 * @property recyclePath Absolute path where the recycled file currently resides.
 * @property expireTime Timestamp after which this item may be permanently deleted (millis since epoch).
 */
@Entity(tableName = "recycle_item")
data class RecycleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "original_id")
    val originalId: Long,

    @ColumnInfo(name = "uri")
    val uri: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "size")
    val size: Long,

    @ColumnInfo(name = "mime_type")
    val mimeType: String? = null,

    @ColumnInfo(name = "deleted_time")
    val deletedTime: Long,

    @ColumnInfo(name = "recycle_path")
    val recyclePath: String,

    @ColumnInfo(name = "expire_time")
    val expireTime: Long
)
