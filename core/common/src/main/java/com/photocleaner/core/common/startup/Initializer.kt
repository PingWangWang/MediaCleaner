/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * App Startup 初始化器，异步初始化日志等组件
 *
 * @author PhotoCleaner
 */
package com.photocleaner.core.common.startup

import android.content.Context
import androidx.startup.Initializer
import com.photocleaner.core.common.utils.LoggingManager

/**
 * 应用启动初始化器。
 *
 * 通过 AndroidX App Startup 库在应用启动时自动执行非核心组件的异步初始化，
 * 避免在 Application.onCreate 中集中初始化导致冷启动时间增加。
 *
 * 当前执行：
 * - 日志系统初始化
 * - 设备性能分级（懒加载）
 *
 * @author PhotoCleaner
 */
class PhotoCleanerInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        // 初始化日志系统
        LoggingManager.initialize(context)
        LoggingManager.i(TAG, "PhotoCleaner 初始化完成")
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        // 无前置依赖
        return emptyList()
    }

    companion object {
        private const val TAG = "PhotoCleanerInit"
    }
}
