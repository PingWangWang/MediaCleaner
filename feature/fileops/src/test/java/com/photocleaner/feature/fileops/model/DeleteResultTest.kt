package com.photocleaner.feature.fileops.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteResultTest {

    @Test
    fun testSuccessResult() {
        val result = DeleteResult.SUCCESS(imageId = 42L, savedBytes = 1024 * 500)
        assertEquals(42L, result.imageId)
        assertEquals(1024 * 500, result.savedBytes)
    }

    @Test
    fun testFailedResult() {
        val result = DeleteResult.FAILED(imageId = 99L, errorMessage = "权限不足")
        assertEquals(99L, result.imageId)
        assertEquals("权限不足", result.errorMessage)
    }

    @Test
    fun testSuccessWithZeroBytes() {
        val result = DeleteResult.SUCCESS(imageId = 1L, savedBytes = 0)
        assertEquals(0, result.savedBytes)
    }
}
