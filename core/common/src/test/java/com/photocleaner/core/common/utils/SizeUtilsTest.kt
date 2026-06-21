package com.photocleaner.core.common.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class SizeUtilsTest {

    @Test
    fun testFormatBytesZero() {
        assertEquals("0 B", SizeUtils.formatBytes(0))
    }

    @Test
    fun testFormatBytesKB() {
        assertEquals("1.0 KB", SizeUtils.formatBytes(1024))
    }

    @Test
    fun testFormatBytesMB() {
        assertEquals("1.0 MB", SizeUtils.formatBytes(1024 * 1024))
    }

    @Test
    fun testFormatBytesGB() {
        assertEquals("1.0 GB", SizeUtils.formatBytes(1024L * 1024 * 1024))
    }

    @Test
    fun testBytesToKB() {
        assertEquals(1, SizeUtils.bytesToKB(1024))
        assertEquals(0, SizeUtils.bytesToKB(512))
        assertEquals(100, SizeUtils.bytesToKB(102400))
    }

    @Test
    fun testBytesToMB() {
        assertEquals(1.0f, SizeUtils.bytesToMB(1024 * 1024), 0.001f)
        assertEquals(0.5f, SizeUtils.bytesToMB(512 * 1024), 0.001f)
    }
}
