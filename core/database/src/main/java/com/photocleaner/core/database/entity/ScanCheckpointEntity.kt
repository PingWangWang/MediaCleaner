/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 扫描断点 Room 实体
 *
 * @author PhotoCleaner
 */
package com.photocleaner.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 扫描检查点实体 - 用于增量扫描断点续扫。
 *
 * 记录每次扫描的状态，当扫描过程中进程被杀或取消时，
 * 下次启动可从断点继续而非重新全量扫描。
 */
@Entity(tableName = "scan_checkpoint")
data class ScanCheckpointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "scan_id")
    val scanId: String, // UUID 标识一次扫描任务

    @ColumnInfo(name = "scan_type")
    val scanType: String, // "FULL" 或 "INCREMENTAL"

    @ColumnInfo(name = "last_processed_id")
    val lastProcessedId: Long, // 最后处理的 MediaStore _ID

    @ColumnInfo(name = "total_count")
    val totalCount: Int, // 本次扫描总图片数

    @ColumnInfo(name = "processed_count")
    val processedCount: Int, // 已处理的图片数

    @ColumnInfo(name = "status")
    val status: String, // "IN_PROGRESS", "COMPLETED", "CANCELLED"

    @ColumnInfo(name = "started_at")
    val startedAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
