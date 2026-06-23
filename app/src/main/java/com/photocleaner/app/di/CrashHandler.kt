/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 全局崩溃捕获处理器
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app.di

import android.content.Context
import android.os.Looper
import android.util.Log
import android.widget.Toast

/**
 * 全局未捕获异常处理器。
 *
 * 当应用发生未捕获异常时，记录崩溃信息、向用户显示友好提示，
 * 然后交由前一个默认处理器处理（或直接杀死进程）。
 *
 * 使用 [install] 方法注册为默认未捕获异常处理器。
 *
 * @author PhotoCleaner
 */
object PhotoCleanerCrashHandler : Thread.UncaughtExceptionHandler {

    private const val TAG = "PhotoCleanerCrash"
    private var previousHandler: Thread.UncaughtExceptionHandler? = null
    private var applicationContext: Context? = null

    /**
     * 安装全局未捕获异常处理器。
     *
     * 应在 [android.app.Application.onCreate] 中尽早调用。
     *
     * @param context Application context
     */
    fun install(context: Context) {
        applicationContext = context.applicationContext
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        Log.i(TAG, "PhotoCleanerCrashHandler 已安装")
    }

    /**
     * 当线程因未捕获异常而终止时调用。
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // 1. 记录崩溃日志
            Log.e(TAG, "应用发生未捕获异常\n" +
                    "线程: ${thread.name} (${thread.id})\n" +
                    "异常: ${throwable.javaClass.name}: ${throwable.message}",
                throwable)

            // 2. 在主线程上显示用户友好提示
            val context = applicationContext
            if (context != null) {
                val mainHandler = android.os.Handler(Looper.getMainLooper())
                mainHandler.post {
                    Toast.makeText(
                        context,
                        "应用出现意外错误，即将重新启动...",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // 3. 延迟一小段时间让 Toast 显示
                try {
                    Thread.sleep(500)
                } catch (_: InterruptedException) {
                    // 忽略
                }
            }
        } catch (e: Exception) {
            // 确保崩溃处理本身不导致二次崩溃
            Log.e(TAG, "CrashHandler 自身发生异常", e)
        } finally {
            // 4. 交由前一个处理器处理，或终止进程
            previousHandler?.uncaughtException(thread, throwable)
                ?: android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
