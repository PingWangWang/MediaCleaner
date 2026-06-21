package com.photocleaner.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a group of duplicate or similar images in the [duplicate_group] table.
 *
 * Each group collects one or more images considered near-duplicates by the detection
 * pipeline. The group carries type/similarity metadata so consumers (e.g. the file-ops
 * module) can decide which image to keep and which to recycle.
 *
 * @property id Auto-generated primary key.
 * @property groupType Group type: 0=EXACT_DUPLICATE, 1=HIGH_SIMILARITY, 2=MEDIUM_SIMILARITY.
 * @property similarity Aggregate similarity score (0.0 – 1.0) for the group.
 * @property memberCount Number of images in this group.
 * @property avgSimilarity Average pairwise similarity within the group (0-100).
 * @property isIgnored Whether the user has marked this group as "not duplicate" (whitelist).
 * @property bestImageId The image id designated as the "best" (highest quality) in this group.
 * @property totalSize Sum of file sizes of all members (bytes); useful for storage-saved estimates.
 * @property createdAt Timestamp when this group was created (millis since epoch).
 */
@Entity(tableName = "duplicate_group")
data class DuplicateGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "group_type")
    val groupType: Int, // 0=EXACT, 1=HIGH, 2=MEDIUM

    @ColumnInfo(name = "similarity")
    val similarity: Float,

    @ColumnInfo(name = "member_count")
    val memberCount: Int = 0,

    @ColumnInfo(name = "avg_similarity")
    val avgSimilarity: Int = 100,

    @ColumnInfo(name = "is_ignored")
    val isIgnored: Boolean = false,

    @ColumnInfo(name = "best_image_id")
    val bestImageId: Long? = null,

    @ColumnInfo(name = "total_size")
    val totalSize: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Long
)
