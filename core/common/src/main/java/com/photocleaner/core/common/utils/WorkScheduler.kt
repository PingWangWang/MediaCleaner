package com.photocleaner.core.common.utils

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WorkManager 后台任务调度器。
 *
 * 负责注册和管理应用的后台周期性任务：
 * - 回收站自动清理（每日）
 * - 后台增量扫描（可选）
 *
 * @author PhotoCleaner
 */
@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val RECYCLE_CLEANUP_WORK_NAME = "recycle_auto_cleanup"
        private const val PERIODIC_SCAN_WORK_NAME = "periodic_incremental_scan"
        private const val CLEANUP_INTERVAL_HOURS = 24L
    }

    /**
     * 安排每日回收站自动清理任务。
     *
     * 约束条件：
     * - 设备正在充电
     * - 设备处于空闲状态
     */
    fun scheduleRecycleCleanup() {
        val constraints = Constraints.Builder()
            .setRequiresCharging(true)
            .setRequiresDeviceIdle(true)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<RecycleCleanupWorker>(
            CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                1, TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RECYCLE_CLEANUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * 取消回收站自动清理任务。
     */
    fun cancelRecycleCleanup() {
        WorkManager.getInstance(context).cancelUniqueWork(RECYCLE_CLEANUP_WORK_NAME)
    }
}
