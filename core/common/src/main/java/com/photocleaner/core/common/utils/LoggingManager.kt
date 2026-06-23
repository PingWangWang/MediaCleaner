package com.photocleaner.core.common.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日志管理器。
 *
 * 提供分级日志输出，支持控制台输出和本地文件持久化。
 * - Debug 构建：所有级别日志输出到控制台
 * - Release 构建：仅 Error 级别输出到控制台
 * - 本地日志文件：运行时日志写入文件，最多保留 7 天
 *
 * @author PhotoCleaner
 */
@Singleton
class LoggingManager @Inject constructor() {

    companion object {
        private const val TAG = "PhotoCleaner"
        private const val LOG_DIR = "logs"
        private const val MAX_LOG_AGE_DAYS = 7
        private const val FILE_NAME_FORMAT = "yyyy-MM-dd"

        @Volatile
        private var isDebug = true

        @Volatile
        private var logDir: File? = null

        enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

        /**
         * 初始化日志系统。由 [com.photocleaner.core.common.startup.PhotoCleanerInitializer] 调用。
         */
        fun initialize(context: Context) {
            logDir = File(context.filesDir, LOG_DIR)
            logDir?.mkdirs()
            isDebug = context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        }

        fun v(tag: String, message: String) = log(LogLevel.VERBOSE, tag, message)
        fun d(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
        fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)
        fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)
        fun e(tag: String, message: String) = log(LogLevel.ERROR, tag, message)

        private fun log(level: LogLevel, tag: String, message: String) {
            // 控制台输出
            if (isDebug || level >= LogLevel.ERROR) {
                when (level) {
                    LogLevel.VERBOSE -> Log.v(tag, message)
                    LogLevel.DEBUG -> Log.d(tag, message)
                    LogLevel.INFO -> Log.i(tag, message)
                    LogLevel.WARN -> Log.w(tag, message)
                    LogLevel.ERROR -> Log.e(tag, message)
                }
            }

            // 文件输出（所有级别均写入文件）
            logToFile(tag, level, message)
        }

        private fun logToFile(tag: String, level: LogLevel, message: String) {
            val dir = logDir ?: return
            val dateStr = SimpleDateFormat(FILE_NAME_FORMAT, Locale.getDefault()).format(Date())
            val logFile = File(dir, "$dateStr.log")

            try {
                FileWriter(logFile, true).use { writer ->
                    val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                    writer.write("$timeStr [${level.name}] [$tag] $message\n")
                }
            } catch (_: Exception) {
                // 日志写入失败不应影响主流程
            }
        }
    }

    /**
     * 导出日志文件供用户反馈。
     */
    fun exportLogs(): File? {
        val dir = logDir ?: return null
        val files = dir.listFiles { f -> f.extension == "log" } ?: return null
        return if (files.isNotEmpty()) files.first() else null
    }

    /**
     * 清理超过指定天数的旧日志文件。
     */
    fun clearOldLogs(maxAgeDays: Int = MAX_LOG_AGE_DAYS) {
        val dir = logDir ?: return
        val cutoff = System.currentTimeMillis() - maxAgeDays * 24L * 60L * 60L * 1000L
        dir.listFiles { f -> f.extension == "log" && f.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }
}
