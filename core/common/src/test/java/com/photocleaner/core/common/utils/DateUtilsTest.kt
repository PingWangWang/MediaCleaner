package com.photocleaner.core.common.utils

import org.junit.Assert.*
import org.junit.Test

class DateUtilsTest {

    @Test
    fun testFormatTimestamp() {
        val millis = 1700000000000L // 2023-11-14 22:13:20 UTC
        val result = DateUtils.formatTimestamp(millis)
        // Should match yyyy-MM-dd HH:mm format
        assertTrue(result.matches(Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")))
    }

    @Test
    fun testIsSameDay_returnsTrueForSameDay() {
        val base = System.currentTimeMillis()
        val sameDayLater = base + 3600_000 // 1 hour later
        assertTrue(DateUtils.isSameDay(base, sameDayLater))
    }

    @Test
    fun testIsSameDay_returnsFalseForDifferentDays() {
        val base = System.currentTimeMillis()
        val nextDay = base + 86400_000L * 2 // 2 days later
        assertFalse(DateUtils.isSameDay(base, nextDay))
    }

    @Test
    fun testFormatRelativeTime() {
        val now = System.currentTimeMillis()
        val fiveMinutesAgo = now - 5 * 60 * 1000
        val result = DateUtils.formatRelativeTime(fiveMinutesAgo)
        // Should return a non-empty string
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }
}
