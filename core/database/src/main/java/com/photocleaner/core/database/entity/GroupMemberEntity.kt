/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 分组成员关系 Room 实体
 *
 * @author PhotoCleaner
 */
package com.photocleaner.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing one member of a duplicate group in the [group_member] table.
 *
 * A many-to-many join table (group → images) with per-member similarity metadata.
 * When a [DuplicateGroupEntity] is deleted, all its members are automatically removed
 * via CASCADE.
 *
 * @property id Auto-generated primary key.
 * @property groupId Foreign key referencing [DuplicateGroupEntity.id].
 * @property imageId Foreign key referencing [ImageItemEntity.id].
 * @property similarity Pairwise similarity between this image and the group's best image.
 * @property isBestImage Whether this member is the designated best image for the group.
 * @property isUserSelected Whether the user manually selected this image as the one to keep.
 * @property sortOrder Display/priority order within the group (lower = higher priority).
 */
@Entity(
    tableName = "group_member",
    foreignKeys = [
        ForeignKey(
            entity = DuplicateGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["group_id"]),
        Index(value = ["group_id", "image_id"], unique = true)
    ]
)
data class GroupMemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "group_id")
    val groupId: Long,

    @ColumnInfo(name = "image_id")
    val imageId: Long,

    @ColumnInfo(name = "similarity")
    val similarity: Int, // 0-100, matches design document

    @ColumnInfo(name = "is_best_image")
    val isBestImage: Boolean = false,

    @ColumnInfo(name = "user_selected")
    val userSelected: Boolean = false,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0
)
