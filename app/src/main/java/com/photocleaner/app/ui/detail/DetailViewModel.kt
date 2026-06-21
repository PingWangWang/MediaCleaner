package com.photocleaner.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.core.common.model.ImageItem
import com.photocleaner.core.database.dao.DuplicateGroupDao
import com.photocleaner.core.database.dao.ImageDao
import com.photocleaner.core.database.entity.toImageItem
import com.photocleaner.feature.fileops.domain.FileOperator
import com.photocleaner.feature.fileops.model.DeleteResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 详情页面 ViewModel。
 *
 * 展示某一重复分组内的所有图片，支持选择保留图片和批量删除。
 */
@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val imageDao: ImageDao,
    private val duplicateGroupDao: DuplicateGroupDao,
    private val fileOperator: FileOperator
) : ViewModel() {

    /** 当前分组 ID（从导航参数获取） */
    val groupId: Long = savedStateHandle.get<Long>("groupId") ?: 0L

    /** 分组中的图片列表 */
    private val _images = MutableStateFlow<List<ImageItem>>(emptyList())
    val images: StateFlow<List<ImageItem>> = _images.asStateFlow()

    /** 用户选中的图片 ID 集合 */
    private val _selectedImageIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedImageIds: StateFlow<Set<Long>> = _selectedImageIds.asStateFlow()

    /** 用户指定的保留图片 ID */
    private val _bestImageId = MutableStateFlow<Long?>(null)
    val bestImageId: StateFlow<Long?> = _bestImageId.asStateFlow()

    /** 是否有选中项 */
    val hasSelection: Boolean
        get() = _selectedImageIds.value.isNotEmpty()

    /** 选中图片的总大小 */
    val selectedTotalSize: Long
        get() = _images.value
            .filter { it.id in _selectedImageIds.value }
            .sumOf { it.size }

    init {
        loadGroupImages()
    }

    /**
     * 从数据库加载指定分组的图片列表。
     */
    private fun loadGroupImages() {
        viewModelScope.launch {
            try {
                val groupWithMembers = duplicateGroupDao.getGroupWithMembers(groupId)
                if (groupWithMembers != null) {
                    val imageIds = groupWithMembers.members.map { it.imageId }
                    val items = imageIds.mapNotNull { id ->
                        imageDao.getById(id)?.toImageItem()
                    }
                    _images.value = items

                    // 如果数据库中有标记保留的图片，设为最佳
                    val bestMember = groupWithMembers.members.find { it.isBestImage }
                    if (bestMember != null) {
                        _bestImageId.value = bestMember.imageId
                    } else if (items.isNotEmpty()) {
                        // 默认第一张为保留图片
                        _bestImageId.value = items.first().id
                    }
                }
            } catch (e: Exception) {
                // 加载失败时保持空列表
            }
        }
    }

    /**
     * 切换图片选中状态。
     *
     * @param id 目标图片 ID
     */
    fun toggleSelection(id: Long) {
        _selectedImageIds.value = if (id in _selectedImageIds.value) {
            _selectedImageIds.value - id
        } else {
            _selectedImageIds.value + id
        }
    }

    /**
     * 设置保留图片。
     *
     * 保留图片不会被删除，且从选中列表中移除。
     *
     * @param id 要设为保留的图片 ID
     */
    fun setBestImage(id: Long) {
        val previousBest = _bestImageId.value
        _bestImageId.value = id
        _selectedImageIds.value = _selectedImageIds.value - id

        // 将之前的保留图片加入可选列表
        if (previousBest != null && previousBest != id) {
            // 之前的保留图片现在可以被选中删除
        }
    }

    /**
     * 删除当前选中的图片（保留图片除外）。
     *
     * 如果选中了保留图片，保留图片不会实际删除。
     *
     * @return 发射每条删除操作结果的 Flow
     */
    fun deleteSelected(): Flow<DeleteResult> = kotlinx.coroutines.flow.flow {
        val bestId = _bestImageId.value
        val imagesToDelete = _images.value
            .filter { it.id in _selectedImageIds.value && it.id != bestId }

        if (imagesToDelete.isEmpty()) return@flow

        for (image in imagesToDelete) {
            val result = fileOperator.deleteImage(image)
            emit(result)

            // 删除成功后，从本地列表中移除
            if (result is DeleteResult.SUCCESS) {
                _images.value = _images.value.filter { it.id != image.id }
                _selectedImageIds.value = _selectedImageIds.value - image.id
            }
        }
    }

    /**
     * 清除所有选中状态。
     */
    fun clearSelection() {
        _selectedImageIds.value = emptySet()
    }
}
