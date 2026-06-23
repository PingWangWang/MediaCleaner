/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 日期工具类
 *
 * @author PhotoCleaner
 */
package com.photocleaner.core.common.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 日期时间工具类。
 *
 * 提供时间戳格式化、相对时间描述（中文）、日期比较等静态方法。
 *
 * @author PhotoCleaner
 */
object DateUtils {

    private const val FORMAT_PATTERN = "yyyy-MM-dd HH:mm"
    private const val MILLIS_IN_SECOND = 1000L
    private const val MILLIS_IN_MINUTE = 60L * 1000
    private const val MILLIS_IN_HOUR = 60L * 60 * 1000
    private const val MILLIS_IN_DAY = 24L * 60 * 60 * 1000

    private val dateFormat = SimpleDateFormat(FORMAT_PATTERN, Locale.getDefault())

    /**
     * 将时间戳格式化为 "yyyy-MM-dd HH:mm" 格式字符串。
     *
     * @param millis 时间戳（毫秒）
     * @return 格式化后的日期时间字符串，如 "2025-06-18 15:30"
     */
    fun formatTimestamp(millis: Long): String {
        return dateFormat.format(Date(millis))
    }

    /**
     * 将时间戳转换为中文相对时间描述。
     *
     * 根据与当前时间的差值返回不同的描述：
     * - 刚刚（< 1 分钟）
     * - X 分钟前（< 1 小时）
     * - X 小时前（< 1 天）
     * - X 天前（< 30 天）
     * - X 个月前（< 12 个月）
     * - X 年前（>= 12 个月）
     *
     * @param millis 时间戳（毫秒）
     * @return 中文相对时间字符串
     */
    fun formatRelativeTime(millis: Long): String {
        val now = System.currentTimeMillis()
        val delta = now - millis

        return when {
            delta < MILLIS_IN_MINUTE -> "刚刚"
            delta < MILLIS_IN_HOUR -> "${delta / MILLIS_IN_MINUTE} 分钟前"
            delta < MILLIS_IN_DAY -> "${delta / MILLIS_IN_HOUR} 小时前"
            delta < MILLIS_IN_DAY * 30 -> "${delta / MILLIS_IN_DAY} 天前"
            delta < MILLIS_IN_DAY * 365 -> "${delta / (MILLIS_IN_DAY * 30)} 个月前"
            else -> "${delta / (MILLIS_IN_DAY * 365)} 年前"
        }
    }

    /**
     * 判断两个时间戳是否在同一天。
     *
     * 基于系统默认时区的日历日期进行比较，忽略时分秒。
     *
     * @param millis1 第一个时间戳（毫秒）
     * @param millis2 第二个时间戳（毫秒）
     * @return 如果两个时间戳在同一天则返回 true，否则 false
     */
    fun isSameDay(millis1: Long, millis2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply {
            timeInMillis = millis1
        }
        val cal2 = Calendar.getInstance().apply {
            timeInMillis = millis2
        }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
