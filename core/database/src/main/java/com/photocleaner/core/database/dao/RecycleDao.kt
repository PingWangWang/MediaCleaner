/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 回收站数据访问接口
 *
 * @author PhotoCleaner
 */
package com.photocleaner.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.photocleaner.core.database.entity.RecycleItemEntity

/**
 * Data-access object for the [RecycleItemEntity] table.
 *
 * Manages soft-deleted images that have been moved to the recycle area.
 * Provides queries for listing, restoring, and purging expired items.
 */
@Dao
interface RecycleDao {

    /**
     * Insert a recycled item record.
     *
     * @param item The [RecycleItemEntity] to persist.
     */
    @Insert
    suspend fun insert(item: RecycleItemEntity)

    /**
     * Retrieve every recycled item, ordered by deletion time descending (newest first).
     */
    @Query("SELECT * FROM recycle_item ORDER BY deleted_time DESC")
    suspend fun getAll(): List<RecycleItemEntity>

    /**
     * Retrieve a single recycled item by its primary key.
     *
     * @param id The item id.
     * @return The matching entity, or null if not found.
     */
    @Query("SELECT * FROM recycle_item WHERE id = :id")
    suspend fun getById(id: Long): RecycleItemEntity?

    /**
     * Delete a single recycled item by id.
     *
     * @param id The item id to remove.
     */
    @Query("DELETE FROM recycle_item WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Permanently remove all items whose [RecycleItemEntity.expireTime] is
     * before the given timestamp.
     *
     * @param now Current timestamp (millis since epoch); items with
     *            expire_time < now will be purged.
     */
    @Query("DELETE FROM recycle_item WHERE expire_time < :now")
    suspend fun deleteExpired(now: Long)

    /**
     * Return the total number of recycled items.
     */
    @Query("SELECT COUNT(*) FROM recycle_item")
    suspend fun count(): Int
}
