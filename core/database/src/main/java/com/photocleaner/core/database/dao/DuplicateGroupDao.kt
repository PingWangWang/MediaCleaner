package com.photocleaner.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.photocleaner.core.database.entity.DuplicateGroupEntity
import com.photocleaner.core.database.entity.GroupMemberEntity

/**
 * Data-access object for duplicate-group operations.
 *
 * Manages both [DuplicateGroupEntity] rows and their associated [GroupMemberEntity]
 * rows. The foreign-key CASCADE on [GroupMemberEntity.groupId] ensures members are
 * removed automatically when a group is deleted.
 */
@Dao
interface DuplicateGroupDao {

    /**
     * Insert a new duplicate group and return its auto-generated id.
     *
     * @param group The group to insert.
     * @return The generated primary key.
     */
    @Insert
    suspend fun insertGroup(group: DuplicateGroupEntity): Long

    /**
     * Insert a batch of group members.
     *
     * @param members List of [GroupMemberEntity] to persist.
     */
    @Insert
    suspend fun insertMembers(members: List<GroupMemberEntity>)

    /**
     * Retrieve all duplicate groups, ordered by creation time descending (newest first).
     */
    @Query("SELECT * FROM duplicate_group ORDER BY created_at DESC")
    suspend fun getAllGroups(): List<DuplicateGroupEntity>

    /**
     * Retrieve a single group by its primary key.
     *
     * @param groupId The group id.
     * @return The matching group, or null if not found.
     */
    @Query("SELECT * FROM duplicate_group WHERE id = :groupId")
    suspend fun getGroupById(groupId: Long): DuplicateGroupEntity?

    /**
     * Retrieve all members belonging to a given group, ordered by [GroupMemberEntity.sortOrder].
     *
     * @param groupId The parent group id.
     * @return List of members, ordered by sort_order ascending.
     */
    @Query("SELECT * FROM group_member WHERE group_id = :groupId ORDER BY sort_order ASC")
    suspend fun getMembersByGroupId(groupId: Long): List<GroupMemberEntity>

    /**
     * Retrieve a group together with all its members in a single transaction.
     *
     * @param groupId The group id.
     * @return A [GroupWithMembers] result, or null if the group does not exist.
     */
    @Transaction
    @Query("SELECT * FROM duplicate_group WHERE id = :groupId")
    suspend fun getGroupWithMembers(groupId: Long): GroupWithMembers?

    /**
     * Retrieve all groups together with their members in a single transaction.
     */
    @Transaction
    @Query("SELECT * FROM duplicate_group ORDER BY created_at DESC")
    suspend fun getAllGroupsWithMembers(): List<GroupWithMembers>

    /**
     * Delete a single group. Member rows are removed via CASCADE.
     *
     * @param group The group entity to delete.
     */
    @Delete
    suspend fun deleteGroup(group: DuplicateGroupEntity)

    /**
     * Delete every group and (via CASCADE) all members.
     */
    @Query("DELETE FROM duplicate_group")
    suspend fun deleteAllGroups()
}

/**
 * Relationship POJO that pairs a [DuplicateGroupEntity] with its [GroupMemberEntity] list.
 */
data class GroupWithMembers(
    val group: DuplicateGroupEntity,
    val members: List<GroupMemberEntity>
)
