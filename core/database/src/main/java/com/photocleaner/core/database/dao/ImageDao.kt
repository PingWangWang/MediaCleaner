package com.photocleaner.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photocleaner.core.database.entity.ImageItemEntity

/**
 * Data-access object for the [ImageItemEntity] table.
 *
 * Provides queries for inserting, updating, and retrieving scanned images,
 * as well as bucket-based candidate lookup for near-duplicate detection.
 */
@Dao
interface ImageDao {

    /**
     * Insert or replace a batch of images.
     *
     * @param images List of [ImageItemEntity] to persist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(images: List<ImageItemEntity>)

    /**
     * Insert or replace a single image.
     *
     * @param image [ImageItemEntity] to persist.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(image: ImageItemEntity)

    /**
     * Retrieve every image in the table.
     */
    @Query("SELECT * FROM image_item")
    suspend fun getAll(): List<ImageItemEntity>

    /**
     * Retrieve a single image by its primary key.
     *
     * @param id The MediaStore _ID.
     * @return The matching entity, or null if not found.
     */
    @Query("SELECT * FROM image_item WHERE id = :id")
    suspend fun getById(id: Long): ImageItemEntity?

    /**
     * Retrieve all images belonging to a given size bucket.
     *
     * @param sizeBucket Exponential size bucket index.
     * @return List of images in that bucket.
     */
    @Query("SELECT * FROM image_item WHERE size_bucket = :sizeBucket")
    suspend fun getImagesBySizeBucket(sizeBucket: Int): List<ImageItemEntity>

    /**
     * Retrieve images that fall within both a size bucket and a ratio bucket.
     * Used during duplicate-detection candidate pre-filtering.
     *
     * @param sizeBucket  Target size bucket.
     * @param ratioBucket Target aspect-ratio bucket.
     * @return List of matching images.
     */
    @Query("SELECT * FROM image_item WHERE size_bucket = :sizeBucket AND ratio_bucket = :ratioBucket")
    suspend fun getImagesByBuckets(sizeBucket: Int, ratioBucket: Int): List<ImageItemEntity>

    /**
     * Retrieve images whose hash has not yet been calculated.
     *
     * @param limit Maximum number of rows to return.
     * @return List of uncalculated images.
     */
    @Query("SELECT * FROM image_item WHERE is_calculated = 0 LIMIT :limit")
    suspend fun getUncalculatedImages(limit: Int): List<ImageItemEntity>

    /**
     * Update the d-hash and p-hash fingerprints for a set of image ids.
     *
     * @param dHash  The difference-hash hex string.
     * @param pHash  The perceptual-hash hex string.
     * @param ids    The primary keys of the images to update.
     */
    @Query("UPDATE image_item SET d_hash = :dHash, p_hash = :pHash, is_calculated = 1 WHERE id IN (:ids)")
    suspend fun updateHashes(dHash: String?, pHash: String?, ids: List<Long>)

    /**
     * Return the total number of images in the table.
     */
    @Query("SELECT COUNT(*) FROM image_item")
    suspend fun count(): Int

    /**
     * Delete images whose primary keys are in [ids].
     *
     * @param ids The list of image ids to remove.
     */
    @Query("DELETE FROM image_item WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /**
     * Retrieve images that have been modified after [since].
     * Used by the incremental scan use-case.
     *
     * @param since Timestamp threshold (millis since epoch).
     * @return List of images modified after that timestamp.
     */
    @Query("SELECT * FROM image_item WHERE modify_time > :since")
    suspend fun getImagesModifiedSince(since: Long): List<ImageItemEntity>
}
