/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 回收站自动清理 WorkManager Worker
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.fileops.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.photocleaner.feature.fileops.domain.FileOperator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 回收站自动清理 Worker。
 *
 * 由 WorkManager 周期性调度，清理已过期的回收站文件。
 * 使用 Hilt 注入依赖，通过 [FileOperator] 执行清理。
 *
 * @author PhotoCleaner
 */
@HiltWorker
class RecycleCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val fileOperator: FileOperator
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            fileOperator.clearExpiredItems()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
