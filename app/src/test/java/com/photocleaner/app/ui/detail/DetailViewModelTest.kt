package com.photocleaner.app.ui.detail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.photocleaner.core.common.model.ImageItem
import com.photocleaner.core.database.dao.DuplicateGroupDao
import com.photocleaner.core.database.dao.GroupWithMembers
import com.photocleaner.core.database.dao.ImageDao
import com.photocleaner.core.database.entity.DuplicateGroupEntity
import com.photocleaner.core.database.entity.GroupMemberEntity
import com.photocleaner.core.database.entity.ImageItemEntity
import com.photocleaner.core.database.entity.RecycleItemEntity
import com.photocleaner.feature.fileops.domain.FileOperator
import com.photocleaner.feature.fileops.model.DeleteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * DetailViewModel 单元测试。
 *
 * 使用手动 fake 替代 ImageDao、DuplicateGroupDao 和 FileOperator。
 *
 * @author PhotoCleaner
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    // Fake 依赖
    private val fakeImageDao = FakeImageDao()
    private val fakeDuplicateGroupDao = FakeDuplicateGroupDao()
    private val fakeFileOperator = FakeFileOperator()

    /** 测试用的 SavedStateHandle，groupId = 1L */
    private val savedStateHandle = SavedStateHandle(mapOf("groupId" to 1L))

    /** 延迟初始化——在具体测试中构造 */
    private lateinit var viewModel: DetailViewModel

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

    private val testImageEntities = listOf(
        ImageItemEntity(
            id = 1L, uri = "content://media/1", name = "photo1.jpg",
            size = 1000, modifyTime = 1000, sizeBucket = 1, ratioBucket = 1, scanTime = 1000
        ),
        ImageItemEntity(
            id = 2L, uri = "content://media/2", name = "photo2.jpg",
            size = 2000, modifyTime = 2000, sizeBucket = 1, ratioBucket = 1, scanTime = 2000
        ),
        ImageItemEntity(
            id = 3L, uri = "content://media/3", name = "photo3.jpg",
            size = 3000, modifyTime = 3000, sizeBucket = 1, ratioBucket = 1, scanTime = 3000
        )
    )

    /** 默认分组：3 个成员，无 isBestImage 标记 */
    private val defaultGroupWithMembers = GroupWithMembers(
        group = DuplicateGroupEntity(
            id = 1L, groupType = 0, similarity = 1.0f, memberCount = 3,
            totalSize = 6000, createdAt = 1000
        ),
        members = listOf(
            GroupMemberEntity(id = 1, groupId = 1L, imageId = 1L, similarity = 100, sortOrder = 0),
            GroupMemberEntity(id = 2, groupId = 1L, imageId = 2L, similarity = 95, sortOrder = 1),
            GroupMemberEntity(id = 3, groupId = 1L, imageId = 3L, similarity = 90, sortOrder = 2)
        )
    )

    /**
     * 快速创建 ViewModel，默认使用 [defaultGroupWithMembers]。
     */
    private fun createViewModel(
        groupWithMembers: GroupWithMembers = defaultGroupWithMembers
    ): DetailViewModel {
        fakeDuplicateGroupDao.groupWithMembers = groupWithMembers
        testImageEntities.forEach { fakeImageDao.images[it.id] = it }
        return DetailViewModel(savedStateHandle, fakeImageDao, fakeDuplicateGroupDao, fakeFileOperator)
    }

    // =====================
    // 初始化测试
    // =====================

    @Test
    fun `init - loads images from database and sets first image as best`() = runBlocking {
        viewModel = createViewModel()

        // 等待 init 中的 launch 完成
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.images.value.size)
        assertEquals(listOf(1L, 2L, 3L), viewModel.images.value.map { it.id })
        // 没有 isBestImage 标记，默认第一张为保留图片
        assertEquals(1L, viewModel.bestImageId.value)
    }

    @Test
    fun `init - respects isBestImage flag from database`() = runBlocking {
        val groupWithBest = GroupWithMembers(
            group = defaultGroupWithMembers.group,
            members = listOf(
                GroupMemberEntity(id = 1, groupId = 1L, imageId = 1L, similarity = 100, sortOrder = 0),
                GroupMemberEntity(id = 2, groupId = 1L, imageId = 2L, similarity = 95, isBestImage = true, sortOrder = 1),
                GroupMemberEntity(id = 3, groupId = 1L, imageId = 3L, similarity = 90, sortOrder = 2)
            )
        )
        viewModel = createViewModel(groupWithBest)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2L, viewModel.bestImageId.value)
    }

    @Test
    fun `init - handles null group gracefully`() = runBlocking {
        fakeImageDao.images.clear()
        viewModel = createViewModel(
            GroupWithMembers(
                group = defaultGroupWithMembers.group,
                members = emptyList()
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.images.value.isEmpty())
        assertNull(viewModel.bestImageId.value)
    }

    // =====================
    // toggleSelection 测试
    // =====================

    @Test
    fun `toggleSelection - adds id to selected set`() {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSelection(1L)
        assertTrue(viewModel.selectedImageIds.value.contains(1L))
        assertTrue(viewModel.hasSelection)
    }

    @Test
    fun `toggleSelection - removes id when already selected`() {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSelection(1L) // select
        viewModel.toggleSelection(1L) // deselect
        assertFalse(viewModel.selectedImageIds.value.contains(1L))
        assertFalse(viewModel.hasSelection)
    }

    @Test
    fun `toggleSelection - multiple selections`() {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSelection(1L)
        viewModel.toggleSelection(3L)
        assertEquals(setOf(1L, 3L), viewModel.selectedImageIds.value)
    }

    // =====================
    // setBestImage 测试
    // =====================

    @Test
    fun `setBestImage - updates bestImageId`() {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setBestImage(2L)
        assertEquals(2L, viewModel.bestImageId.value)
    }

    @Test
    fun `setBestImage - removes best image from selected set`() {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSelection(1L)
        viewModel.toggleSelection(2L)
        assertEquals(setOf(1L, 2L), viewModel.selectedImageIds.value)

        viewModel.setBestImage(2L)
        // 2L 应从选中列表中移除
        assertEquals(setOf(1L), viewModel.selectedImageIds.value)
    }

    // =====================
    // clearSelection 测试
    // =====================

    @Test
    fun `clearSelection - empties selected set`() {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSelection(1L)
        viewModel.toggleSelection(2L)
        assertTrue(viewModel.hasSelection)

        viewModel.clearSelection()
        assertTrue(viewModel.selectedImageIds.value.isEmpty())
        assertFalse(viewModel.hasSelection)
    }

    // =====================
    // selectedTotalSize 测试
    // =====================

    @Test
    fun `selectedTotalSize - returns sum of selected image sizes`() {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSelection(1L) // size = 1000
        viewModel.toggleSelection(3L) // size = 3000
        assertEquals(4000L, viewModel.selectedTotalSize)
    }

    @Test
    fun `selectedTotalSize - returns zero when nothing selected`() {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0L, viewModel.selectedTotalSize)
    }

    // =====================
    // deleteSelected 测试
    // =====================

    @Test
    fun `deleteSelected - deletes selected images except best image`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // 默认 bestImageId = 1L（第一张）
        viewModel.toggleSelection(1L)
        viewModel.toggleSelection(2L)
        viewModel.toggleSelection(3L)

        val results = viewModel.deleteSelected().toList()

        // 应删除 2L 和 3L（排除保留图片 1L）
        assertEquals(2, results.size)
        assertTrue(results.all { it is DeleteResult.SUCCESS })
        assertEquals(listOf(2L, 3L), results.map { (it as DeleteResult.SUCCESS).imageId })
    }

    @Test
    fun `deleteSelected - removes deleted images from local state after success`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSelection(2L)
        viewModel.toggleSelection(3L)

        viewModel.deleteSelected().toList()

        // 删除后，2L 和 3L 应从 images 和 selectedImageIds 中移除
        assertEquals(listOf(1L), viewModel.images.value.map { it.id })
        assertTrue(viewModel.selectedImageIds.value.isEmpty())
    }

    @Test
    fun `deleteSelected - does nothing when no images selected`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val results = viewModel.deleteSelected().toList()
        assertTrue(results.isEmpty())
        // 列表保持不变
        assertEquals(3, viewModel.images.value.size)
    }

    @Test
    fun `deleteSelected - only best image selected results in no deletion`() = runBlocking {
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // bestImageId = 1L, 只选中 1L
        viewModel.toggleSelection(1L)

        val results = viewModel.deleteSelected().toList()
        assertTrue(results.isEmpty())
        assertEquals(3, viewModel.images.value.size)
    }

    @Test
    fun `deleteSelected - emits FAILED results when deletion fails`() = runBlocking {
        fakeFileOperator.shouldFailIds = setOf(2L)
        viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.toggleSelection(2L)
        viewModel.toggleSelection(3L)

        val results = viewModel.deleteSelected().toList()

        assertEquals(2, results.size)
        assertTrue(results[0] is DeleteResult.FAILED)
        assertTrue(results[1] is DeleteResult.SUCCESS)

        // 失败时，失败图片不应从列表中移除；成功图片应移除
        assertEquals(2, viewModel.images.value.size)
    }
}

// =============================================================================
// Fake 实现
// =============================================================================

/**
 * Fake [ImageDao]，仅实现 [getById]。
 */
class FakeImageDao : ImageDao {
    val images = mutableMapOf<Long, ImageItemEntity>()

    override suspend fun getById(id: Long): ImageItemEntity? = images[id]

    // 未使用的方法
    override suspend fun insertAll(images: List<ImageItemEntity>) = Unit
    override suspend fun insert(image: ImageItemEntity) = Unit
    override suspend fun getAll(): List<ImageItemEntity> = emptyList()
    override suspend fun getImagesBySizeBucket(sizeBucket: Int): List<ImageItemEntity> = emptyList()
    override suspend fun getImagesByBuckets(sizeBucket: Int, ratioBucket: Int): List<ImageItemEntity> = emptyList()
    override suspend fun getUncalculatedImages(limit: Int): List<ImageItemEntity> = emptyList()
    override suspend fun updateHashes(dHash: String?, pHash: String?, ids: List<Long>) = Unit
    override suspend fun count(): Int = 0
    override suspend fun deleteByIds(ids: List<Long>) = Unit
    override suspend fun getImagesModifiedSince(since: Long): List<ImageItemEntity> = emptyList()
}

/**
 * Fake [DuplicateGroupDao]，仅实现 [getGroupWithMembers]。
 */
class FakeDuplicateGroupDao : DuplicateGroupDao {
    var groupWithMembers: GroupWithMembers? = null

    override suspend fun getGroupWithMembers(groupId: Long): GroupWithMembers? = groupWithMembers

    // 未使用的方法
    override suspend fun insertGroup(group: DuplicateGroupEntity): Long = 0
    override suspend fun insertMembers(members: List<GroupMemberEntity>) = Unit
    override suspend fun getAllGroups(): List<DuplicateGroupEntity> = emptyList()
    override suspend fun getGroupById(groupId: Long): DuplicateGroupEntity? = null
    override suspend fun getMembersByGroupId(groupId: Long): List<GroupMemberEntity> = emptyList()
    override suspend fun getAllGroupsWithMembers(): List<GroupWithMembers> = emptyList()
    override suspend fun deleteGroup(group: DuplicateGroupEntity) = Unit
    override suspend fun deleteAllGroups() = Unit
}

/**
 * Fake [FileOperator]，仅实现 [deleteImage]。
 *
 * 默认对所有图片返回 [DeleteResult.SUCCESS]。
 * 设置 [shouldFailIds] 可使指定 ID 的删除操作返回 FAILED。
 */
class FakeFileOperator : FileOperator {
    var shouldFailIds: Set<Long> = emptySet()

    override suspend fun deleteImage(image: ImageItem): DeleteResult {
        return if (image.id in shouldFailIds) {
            DeleteResult.FAILED(imageId = image.id, errorMessage = "模拟失败")
        } else {
            DeleteResult.SUCCESS(imageId = image.id, savedBytes = image.size)
        }
    }

    override suspend fun deleteImages(images: List<ImageItem>): List<DeleteResult> {
        return images.map { deleteImage(it) }
    }

    // 未使用的方法
    override suspend fun restoreImage(recycleItemId: Long): Boolean = true
    override fun getRecycleBinItems(): Flow<List<RecycleItemEntity>> = kotlinx.coroutines.flow.flow { emit(emptyList()) }
    override suspend fun clearExpiredItems() = Unit
    override suspend fun permanentlyDelete(itemId: Long) = Unit
    override fun getRecycleCount(): Flow<Int> = kotlinx.coroutines.flow.flow { emit(0) }
}
