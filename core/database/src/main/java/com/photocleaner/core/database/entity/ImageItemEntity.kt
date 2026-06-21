package com.photocleaner.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.photocleaner.core.common.model.ImageItem

/**
 * Room entity representing a scanned image in the [image_item] table.
 *
 * Each row corresponds to one media-store image that has been indexed.
 * Bucket columns (size_bucket, ratio_bucket) enable efficient near-duplicate
 * candidate lookup without cross-joining every row.
 *
 * @property id MediaStore _ID of the image.
 * @property uri Content URI string, e.g. content://media/external/images/media/123.
 * @property path Absolute file path on disk (nullable, may not always be accessible).
 * @property name Display name of the image file.
 * @property size File size in bytes.
 * @property width Image width in pixels (nullable before calculation).
 * @property height Image height in pixels (nullable before calculation).
 * @property mimeType MIME type of the image, e.g. image/jpeg.
 * @property modifyTime Last-modified timestamp (millis since epoch).
 * @property dHash Difference-hash fingerprint (hex string). Null until calculated.
 * @property pHash Perceptual-hash fingerprint (hex string). Null until calculated.
 * @property sizeBucket Exponential size bucket index based on log2(size).
 * @property ratioBucket Discrete aspect-ratio bucket based on width/height.
 * @property orientation EXIF orientation value (0 = unknown, 1-8 = standard EXIF).
 * @property isCalculated Whether hash/fingerprint calculation has been performed.
 * @property scanTime Timestamp when this row was inserted/updated (millis since epoch).
 */
@Entity(
    tableName = "image_item",
    indices = [
        Index(value = ["modify_time"]),
        Index(value = ["size_bucket"]),
        Index(value = ["ratio_bucket"])
    ]
)
data class ImageItemEntity(
    @PrimaryKey
    val id: Long,

    @ColumnInfo(name = "uri")
    val uri: String,

    @ColumnInfo(name = "path")
    val path: String? = null,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "size")
    val size: Long,

    @ColumnInfo(name = "width")
    val width: Int? = null,

    @ColumnInfo(name = "height")
    val height: Int? = null,

    @ColumnInfo(name = "mime_type")
    val mimeType: String? = null,

    @ColumnInfo(name = "modify_time")
    val modifyTime: Long,

    @ColumnInfo(name = "d_hash")
    val dHash: String? = null,

    @ColumnInfo(name = "p_hash")
    val pHash: String? = null,

    @ColumnInfo(name = "size_bucket")
    val sizeBucket: Int,

    @ColumnInfo(name = "ratio_bucket")
    val ratioBucket: Int,

    @ColumnInfo(name = "orientation")
    val orientation: Int = 0,

    @ColumnInfo(name = "is_calculated")
    val isCalculated: Boolean = false,

    @ColumnInfo(name = "scan_time")
    val scanTime: Long
)

/**
 * Converts this Room entity to the domain [ImageItem] model.
 */
fun ImageItemEntity.toImageItem(): ImageItem = ImageItem(
    id = id,
    uri = uri,
    path = path,
    name = name,
    size = size,
    width = width,
    height = height,
    mimeType = mimeType,
    modifyTime = modifyTime,
    dHash = dHash,
    pHash = pHash,
    sizeBucket = sizeBucket,
    ratioBucket = ratioBucket,
    orientation = orientation,
    isCalculated = isCalculated,
    scanTime = scanTime
)

/**
 * Companion factory that creates an [ImageItemEntity] from a domain [ImageItem].
 */
fun ImageItemEntity.Companion.fromImageItem(item: ImageItem): ImageItemEntity = ImageItemEntity(
    id = item.id,
    uri = item.uri,
    path = item.path,
    name = item.name,
    size = item.size,
    width = item.width,
    height = item.height,
    mimeType = item.mimeType,
    modifyTime = item.modifyTime,
    dHash = item.dHash,
    pHash = item.pHash,
    sizeBucket = item.sizeBucket,
    ratioBucket = item.ratioBucket,
    orientation = item.orientation,
    isCalculated = item.isCalculated,
    scanTime = item.scanTime
)
