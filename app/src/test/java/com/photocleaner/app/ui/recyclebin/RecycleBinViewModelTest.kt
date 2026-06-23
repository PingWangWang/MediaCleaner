package com.photocleaner.app.ui.recyclebin

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.photocleaner.core.database.entity.RecycleItemEntity
import com.photocleaner.core.common.model.ImageItem
import com.photocleaner.feature.fileops.domain.FileOperator
import com.photocleaner.feature.fileops.model.DeleteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * RecycleBinViewModel 单元测试。
 *
 * 使用手动 fake 实现替代 FileOperator。
 *
 * @author PhotoCleaner
 */
class RecycleBinViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    /** Fake FileOperator */
    private lateinit var fakeFileOperator: FakeFileOperatorRecycleBin

    private lateinit var viewModel: RecycleBinViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // =====================
    // 测试辅助数据
    // =====================

    private val testRecycleItems = listOf(
        RecycleItemEntity(
            id = 1L, originalId = 101L, uri = "content://media/1",
            name = "deleted1.jpg", size = 1000,
            deletedTime = 1000, recyclePath = "/recycle/1", expireTime = 999999
        ),
        RecycleItemEntity(
            id = 2L, originalId = 102L, uri = "content://media/2",
            name = "deleted2.jpg", size = 2000,
            deletedTime = 2000, recyclePath = "/recycle/2", expireTime = 999999
        ),
        RecycleItemEntity(
            id = 3L, originalId = 103L, uri = "content://media/3",
            name = "deleted3.jpg", size = 3000,
            deletedTime = 3000, recyclePath = "/recycle/3", expireTime = 999999
        )
    )

    /**
     * 创建 ViewModel 并返回它的 FakeFileOperator。
     */
    private fun createViewModel(
        initialItems: List<RecycleItemEntity> = testRecycleItems
    ): RecycleBinViewModel {
        fakeFileOperator = FakeFileOperatorRecycleBin(initialItems)
        viewModel = RecycleBinViewModel(fakeFileOperator)
        // 推进 init 中的 collect
        testDispatcher.scheduler.advanceUntilIdle()
        return viewModel
    }

    // =====================
    // 初始化测试
    // =====================

    @Test
    fun `init - loads recycle bin items automatically`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.itemCount.value)
        assertEquals(3, viewModel.items.value.size)
        assertEquals(listOf(1L, 2L, 3L), viewModel.items.value.map { it.id })
    }

    @Test
    fun `init - shows empty list when no items`() = runBlocking {
        viewModel = createViewModel(initialItems = emptyList())
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.itemCount.value)
        assertTrue(viewModel.items.value.isEmpty())
    }

    @Test
    fun `init - updates items when Flow emits new data`() = runBlocking {
        fakeFileOperator = FakeFileOperatorRecycleBin(testRecycleItems)
        viewModel = RecycleBinViewModel(fakeFileOperator)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.itemCount.value)

        // 模拟 Flow 发射新数据（例如其他页面删除了图片，回收站更新）
        val updatedItems = testRecycleItems.take(2)
        fakeFileOperator._itemsFlow.value = updatedItems
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.itemCount.value)
    }

    // =====================
    // restoreItem 测试
    // =====================

    @Test
    fun `restoreItem - restores item and removes it from list`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        fakeFileOperator.restoreSuccess = true
        viewModel.restoreItem(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        // 1L 应从列表中移除
        assertEquals(2, viewModel.items.value.size)
        assertEquals(listOf(2L, 3L), viewModel.items.value.map { it.id })
        assertEquals(2, viewModel.itemCount.value)
        // 验证调用了 FileOperator.restoreImage
        assertEquals(listOf(1L), fakeFileOperator.restoredIds)
    }

    @Test
    fun `restoreItem - does not remove item when restore fails`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        fakeFileOperator.restoreSuccess = false
        viewModel.restoreItem(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        // 恢复失败，列表保持不变
        assertEquals(3, viewModel.items.value.size)
    }

    @Test
    fun `restoreItem - handles exception gracefully`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        fakeFileOperator.throwOnRestore = true
        viewModel.restoreItem(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        // 异常时列表不变
        assertEquals(3, viewModel.items.value.size)
    }

    // =====================
    // permanentlyDelete 测试
    // =====================

    @Test
    fun `permanentlyDelete - removes item from list`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.permanentlyDelete(2L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.items.value.size)
        assertEquals(listOf(1L, 3L), viewModel.items.value.map { it.id })
        assertEquals(2, viewModel.itemCount.value)
        assertEquals(listOf(2L), fakeFileOperator.permanentlyDeletedIds)
    }

    @Test
    fun `permanentlyDelete - handles exception gracefully`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        fakeFileOperator.throwOnPermanentDelete = true
        viewModel.permanentlyDelete(2L)
        testDispatcher.scheduler.advanceUntilIdle()

        // 异常时列表不变
        assertEquals(3, viewModel.items.value.size)
    }

    // =====================
    // clearAll 测试
    // =====================

    @Test
    fun `clearAll - permanently deletes all items and clears list`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearAll()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.items.value.isEmpty())
        assertEquals(0, viewModel.itemCount.value)
        // 验证每个条目都调用了 permanentlyDelete
        assertEquals(listOf(1L, 2L, 3L), fakeFileOperator.permanentlyDeletedIds)
    }

    @Test
    fun `clearAll - handles exception and keeps original items`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        fakeFileOperator.throwOnPermanentDelete = true
        viewModel.clearAll()
        testDispatcher.scheduler.advanceUntilIdle()

        // 异常时列表应保持不变（catch 块不修改状态）
        assertEquals(3, viewModel.items.value.size)
    }

    @Test
    fun `clearAll - clears empty list without error`() = runBlocking {
        viewModel = createViewModel(initialItems = emptyList())
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearAll()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.items.value.isEmpty())
        assertEquals(0, viewModel.itemCount.value)
    }

    // =====================
    // clearExpired 测试
    // =====================

    @Test
    fun `clearExpired - delegates to fileOperator`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearExpired()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeFileOperator.clearExpiredCalled)
    }

    @Test
    fun `clearExpired - handles exception gracefully`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        fakeFileOperator.throwOnClearExpired = true
        // 不应该抛出异常
        viewModel.clearExpired()
        testDispatcher.scheduler.advanceUntilIdle()
    }
}

// =============================================================================
// Fake FileOperator 实现（专用于 RecycleBinViewModel 测试）
// =============================================================================

/**
 * Fake [FileOperator]，用于 [RecycleBinViewModel] 测试。
 *
 * 使用 [MutableStateFlow] 模拟 [getRecycleBinItems] 的返回数据，
 * 记录 [restoreImage] 和 [permanentlyDelete] 的调用参数以便验证。
 */
class FakeFileOperatorRecycleBin(
    initialItems: List<RecycleItemEntity>
) : FileOperator {

    /** 可变的回收站条目 Flow */
    val _itemsFlow = MutableStateFlow(initialItems)

    // 调用记录
    val restoredIds = mutableListOf<Long>()
    val permanentlyDeletedIds = mutableListOf<Long>()
    var clearExpiredCalled = false

    // 行为控制
    var restoreSuccess = true
    var throwOnRestore = false
    var throwOnPermanentDelete = false
    var throwOnClearExpired = false

    override fun getRecycleBinItems(): Flow<List<RecycleItemEntity>> = _itemsFlow

    override suspend fun restoreImage(recycleItemId: Long): Boolean {
        if (throwOnRestore) throw RuntimeException("模拟恢复异常")
        restoredIds.add(recycleItemId)
        if (restoreSuccess) {
            // 从 Flow 中移除该条目
            _itemsFlow.value = _itemsFlow.value.filter { it.id != recycleItemId }
        }
        return restoreSuccess
    }

    override suspend fun permanentlyDelete(itemId: Long) {
        if (throwOnPermanentDelete) throw RuntimeException("模拟永久删除异常")
        permanentlyDeletedIds.add(itemId)
        _itemsFlow.value = _itemsFlow.value.filter { it.id != itemId }
    }

    override suspend fun clearExpiredItems() {
        if (throwOnClearExpired) throw RuntimeException("模拟清理异常")
        clearExpiredCalled = true
    }

    override suspend fun deleteImage(image: ImageItem): DeleteResult =
        DeleteResult.SUCCESS(imageId = image.id, savedBytes = image.size)

    override suspend fun deleteImages(images: List<ImageItem>): List<DeleteResult> =
        images.map { DeleteResult.SUCCESS(imageId = it.id, savedBytes = it.size) }

    override fun getRecycleCount(): Flow<Int> = _itemsFlow.map { it.size }
}
