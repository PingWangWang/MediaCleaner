/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * ResultViewModel 单元测试
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app.ui.result

import androidx.lifecycle.SavedStateHandle
import com.photocleaner.core.common.model.DuplicateGroup
import com.photocleaner.core.common.model.GroupType
import com.photocleaner.core.common.model.ImageItem
import com.photocleaner.core.database.entity.RecycleItemEntity
import com.photocleaner.feature.fileops.domain.FileOperator
import com.photocleaner.feature.fileops.model.DeleteResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * [ResultViewModel] 的单元测试。
 *
 * 使用手动 fake 实现替代 [FileOperator] 和 [SavedStateHandle]，
 * 验证按钮点击触发的选择/删除逻辑。
 */
class ResultViewModelTest {

    /** 测试用的分组数据 */
    private val testImage1 = ImageItem(
        id = 101L, uri = "content://img/101", name = "img1.jpg",
        size = 1024L, modifyTime = System.currentTimeMillis()
    )
    private val testImage2 = ImageItem(
        id = 102L, uri = "content://img/102", name = "img2.jpg",
        size = 2048L, modifyTime = System.currentTimeMillis()
    )
    private val testImage3 = ImageItem(
        id = 103L, uri = "content://img/103", name = "img3.jpg",
        size = 4096L, modifyTime = System.currentTimeMillis()
    )
    private val testImage4 = ImageItem(
        id = 104L, uri = "content://img/104", name = "img4.jpg",
        size = 8192L, modifyTime = System.currentTimeMillis()
    )

    private val group1 = DuplicateGroup(
        groupId = 1L,
        images = listOf(testImage1, testImage2),
        similarity = 1f,
        groupType = GroupType.EXACT_DUPLICATE,
        bestImage = testImage1,
        canDeleteImages = listOf(testImage2),
        size = 2048L
    )

    private val group2 = DuplicateGroup(
        groupId = 2L,
        images = listOf(testImage3, testImage4),
        similarity = 0.95f,
        groupType = GroupType.HIGH_SIMILARITY,
        bestImage = testImage3,
        canDeleteImages = listOf(testImage4),
        size = 8192L
    )

    private val allGroups = listOf(group1, group2)

    /** 伪造的 FileOperator，追踪 [deleteImage] 调用 */
    private val fakeFileOperator = FakeFileOperator()

    private lateinit var viewModel: ResultViewModel

    @Before
    fun setUp() {
        fakeFileOperator.reset()
        val savedStateHandle = SavedStateHandle(mapOf("groups" to allGroups))
        viewModel = ResultViewModel(
            savedStateHandle = savedStateHandle,
            fileOperator = fakeFileOperator
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Test 1: toggleGroupSelection 切换选中状态
    // ──────────────────────────────────────────────────────────────
    @Test
    fun toggleGroupSelection_togglesSelection() {
        // 初始未选中任何组
        assertTrue(
            "初始应无选中",
            viewModel.selectedGroupIds.value.isEmpty()
        )
        assertFalse("初始 hasSelection 应为 false", viewModel.hasSelection)
        assertFalse("初始 isAllSelected 应为 false", viewModel.isAllSelected)

        // 选中 group1
        viewModel.toggleGroupSelection(1L)
        assertEquals("选中 group1 后应包含 1", setOf(1L), viewModel.selectedGroupIds.value)
        assertTrue("hasSelection 应为 true", viewModel.hasSelection)
        assertEquals("selectedTotalSize 应为 2048", 2048L, viewModel.selectedTotalSize)
        assertEquals("selectedImageCount 应为 1", 1, viewModel.selectedImageCount)

        // 再次切换 group1 → 取消选中
        viewModel.toggleGroupSelection(1L)
        assertTrue(
            "取消选中 group1 后应无选中",
            viewModel.selectedGroupIds.value.isEmpty()
        )

        // 选中 group2
        viewModel.toggleGroupSelection(2L)
        assertEquals("选中 group2 后应包含 2", setOf(2L), viewModel.selectedGroupIds.value)
        assertEquals("selectedTotalSize 应为 8192", 8192L, viewModel.selectedTotalSize)
    }

    // ──────────────────────────────────────────────────────────────
    // Test 2: selectAll 全选所有组
    // ──────────────────────────────────────────────────────────────
    @Test
    fun selectAll_selectsAllGroups() {
        viewModel.selectAll()
        assertEquals(
            "selectAll 应选中所有组",
            setOf(1L, 2L),
            viewModel.selectedGroupIds.value
        )
        assertTrue("isAllSelected 应为 true", viewModel.isAllSelected)
        assertEquals("selectedTotalSize 应包含两组大小", 2048L + 8192L, viewModel.selectedTotalSize)
        assertEquals("selectedImageCount 应包含两张可删图", 2, viewModel.selectedImageCount)
    }

    // ──────────────────────────────────────────────────────────────
    // Test 3: clearSelection 清空选择
    // ──────────────────────────────────────────────────────────────
    @Test
    fun clearSelection_clearsAllSelections() {
        // 先全选
        viewModel.selectAll()
        assertFalse("全选后不应为空", viewModel.selectedGroupIds.value.isEmpty())

        viewModel.clearSelection()
        assertTrue(
            "clearSelection 后应无选中",
            viewModel.selectedGroupIds.value.isEmpty()
        )
        assertFalse("isAllSelected 应为 false", viewModel.isAllSelected)
        assertFalse("hasSelection 应为 false", viewModel.hasSelection)
        assertEquals("selectedTotalSize 应为 0", 0L, viewModel.selectedTotalSize)
        assertEquals("selectedImageCount 应为 0", 0, viewModel.selectedImageCount)
    }

    // ──────────────────────────────────────────────────────────────
    // Test 4: selectAll 再次调用取消全选（切换行为）
    // ──────────────────────────────────────────────────────────────
    @Test
    fun selectAll_toggles_whenAlreadyAllSelected() {
        viewModel.selectAll()
        assertTrue("第一次 selectAll 应全选", viewModel.isAllSelected)

        // 再次调用 selectAll → 取消全选
        viewModel.selectAll()
        assertFalse("再次 selectAll 应取消全选", viewModel.isAllSelected)
        assertTrue(
            "再次 selectAll 后应无选中",
            viewModel.selectedGroupIds.value.isEmpty()
        )
    }

    // ──────────────────────────────────────────────────────────────
    // Test 5: deleteSelected 调用 FileOperator
    // ──────────────────────────────────────────────────────────────
    @Test
    fun deleteSelected_callsFileOperator() = runTest {
        // 选中 group1
        viewModel.toggleGroupSelection(1L)
        assertEquals("应选中 group1", setOf(1L), viewModel.selectedGroupIds.value)

        // 执行删除
        val results = viewModel.deleteSelected().toList()

        // FileOperator.deleteImage 应被调用一次（group1 有一张可删图 testImage2）
        assertEquals("deleteImage 应被调用 1 次", 1, fakeFileOperator.deletedImages.size)
        assertEquals(
            "删除的图片应为 testImage2",
            testImage2.id,
            fakeFileOperator.deletedImages[0].id
        )
        assertEquals("应返回 1 个结果", 1, results.size)
        assertTrue(
            "删除结果应为 SUCCESS",
            results[0] is DeleteResult.SUCCESS
        )
    }

    @Test
    fun deleteSelected_withMultipleGroups_callsFileOperatorMultipleTimes() = runTest {
        // 选中两组
        viewModel.selectAll()

        val results = viewModel.deleteSelected().toList()

        // 两张可删图 → deleteImage 被调用两次
        assertEquals("deleteImage 应被调用 2 次", 2, fakeFileOperator.deletedImages.size)
        assertEquals("应返回 2 个结果", 2, results.size)
    }

    @Test
    fun deleteSelected_withNoSelection_doesNotCallFileOperator() = runTest {
        // 未选中任何组
        val results = viewModel.deleteSelected().toList()

        assertTrue(
            "无选中时不应调用 FileOperator",
            fakeFileOperator.deletedImages.isEmpty()
        )
        assertTrue("应返回空结果", results.isEmpty())
    }

    // ══════════════════════════════════════════════════════════════
    //   Fake 实现
    // ══════════════════════════════════════════════════════════════

    /**
     * 伪造 [FileOperator] 实现，追踪 [deleteImage] 的调用参数。
     */
    private class FakeFileOperator : FileOperator {

        /** 记录所有被传入 [deleteImage] 的图片 */
        val deletedImages = mutableListOf<ImageItem>()

        fun reset() {
            deletedImages.clear()
        }

        override suspend fun deleteImage(image: ImageItem): DeleteResult {
            deletedImages.add(image)
            return DeleteResult.SUCCESS(imageId = image.id, savedBytes = image.size)
        }

        override suspend fun deleteImages(images: List<ImageItem>): List<DeleteResult> {
            return images.map { image ->
                deletedImages.add(image)
                DeleteResult.SUCCESS(imageId = image.id, savedBytes = image.size)
            }
        }

        override suspend fun restoreImage(recycleItemId: Long): Boolean = true

        override fun getRecycleBinItems(): Flow<List<RecycleItemEntity>> = flowOf(emptyList())

        override suspend fun clearExpiredItems() { }

        override suspend fun permanentlyDelete(itemId: Long) { }

        override fun getRecycleCount(): Flow<Int> = flowOf(0)
    }
}
