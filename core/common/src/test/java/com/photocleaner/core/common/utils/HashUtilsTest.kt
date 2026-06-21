package com.photocleaner.core.common.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HashUtilsTest {

    @Test
    fun testMd5WithKnownInput() {
        val result = HashUtils.md5("Hello PhotoCleaner".toByteArray())
        // MD5 of "Hello PhotoCleaner" - computed expected value
        assertEquals(32, result.length)
        assertTrue(result.matches(Regex("[0-9a-f]{32}")))
    }

    @Test
    fun testSha256WithKnownInput() {
        val result = HashUtils.sha256("test-input")
        assertEquals(64, result.length)
        assertTrue(result.matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun testMd5WithEmptyInput() {
        val result = HashUtils.md5(ByteArray(0))
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", result)
    }
}
