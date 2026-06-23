/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 扫描断点数据访问接口
 *
 * @author PhotoCleaner
 */
package com.photocleaner.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.photocleaner.core.database.entity.ScanCheckpointEntity

@Dao
interface ScanCheckpointDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checkpoint: ScanCheckpointEntity)

    @Query("SELECT * FROM scan_checkpoint WHERE scan_id = :scanId LIMIT 1")
    suspend fun getByScanId(scanId: String): ScanCheckpointEntity?

    @Query("SELECT * FROM scan_checkpoint WHERE status = 'IN_PROGRESS' ORDER BY updated_at DESC LIMIT 1")
    suspend fun getLatestInProgress(): ScanCheckpointEntity?

    @Query("UPDATE scan_checkpoint SET status = :status, updated_at = :now WHERE scan_id = :scanId")
    suspend fun updateStatus(scanId: String, status: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE scan_checkpoint SET processed_count = :processedCount, last_processed_id = :lastId, updated_at = :now WHERE scan_id = :scanId")
    suspend fun updateProgress(scanId: String, processedCount: Int, lastId: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM scan_checkpoint")
    suspend fun deleteAll()
}
