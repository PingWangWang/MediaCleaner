package com.photocleaner.core.common.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * 设备性能分级器。
 *
 * 根据 CPU 核心数和可用内存对设备进行分级，
 * 用于自适应调度扫描并发数与算法档位。
 *
 * @author PhotoCleaner
 */
object DeviceClassifier {

    /** 设备性能等级 */
    enum class DeviceTier {
        /** 高端机：CPU >= 8 核 且 内存 >= 8GB */
        HIGH,
        /** 中端机：CPU >= 8 核 且 内存 4~8GB */
        MEDIUM,
        /** 低端机：CPU < 8 核 或 内存 <= 4GB */
        LOW
    }

    /**
     * 分类设备性能等级。
     */
    fun classify(context: Context): DeviceTier {
        val cpuCount = getAvailableProcessors()
        val totalMemGB = getTotalMemoryGB(context)

        return when {
            cpuCount >= 8 && totalMemGB >= 8f -> DeviceTier.HIGH
            cpuCount >= 8 && totalMemGB >= 4f -> DeviceTier.MEDIUM
            else -> DeviceTier.LOW
        }
    }

    /**
     * 根据设备等级获取推荐扫描并发数。
     */
    fun getRecommendedConcurrency(tier: DeviceTier): Int = when (tier) {
        DeviceTier.HIGH -> 6
        DeviceTier.MEDIUM -> 4
        DeviceTier.LOW -> 2
    }

    /**
     * 根据设备等级获取推荐算法档位。
     */
    fun getRecommendedAlgorithmLevel(tier: DeviceTier): String = when (tier) {
        DeviceTier.HIGH -> "full"     // dHash + pHash + ORB
        DeviceTier.MEDIUM -> "standard" // dHash + pHash
        DeviceTier.LOW -> "basic"     // dHash only
    }

    /**
     * 获取可用 CPU 核心数。
     */
    private fun getAvailableProcessors(): Int {
        return Runtime.getRuntime().availableProcessors()
    }

    /**
     * 获取总内存大小（GB）。
     */
    private fun getTotalMemoryGB(context: Context): Float {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.let { it.getMemoryInfo(memoryInfo) }
            memoryInfo.totalMem.toFloat() / (1024f * 1024f * 1024f)
        } catch (e: Exception) {
            // 无法获取内存信息时保守估计为 4GB
            4f
        }
    }
}
