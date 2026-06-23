/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 去重结果页 ViewModel
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.core.common.model.DuplicateGroup
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
 * 结果页面 ViewModel。
 *
 * 展示重复检测分组列表，管理用户选择状态，
 * 提供删除选中组中可删除图片的能力。
 */
@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fileOperator: FileOperator
) : ViewModel() {

    /** 从导航参数中获取的分组列表 */
    val groups: List<DuplicateGroup> =
        savedStateHandle.get<List<DuplicateGroup>>("groups") ?: emptyList()

    /** 用户选中的分组 ID 集合 */
    private val _selectedGroupIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedGroupIds: StateFlow<Set<Long>> = _selectedGroupIds.asStateFlow()

    /** 是否全选 */
    val isAllSelected: Boolean
        get() = groups.isNotEmpty() && _selectedGroupIds.value.size == groups.size

    /** 是否有选中项 */
    val hasSelection: Boolean
        get() = _selectedGroupIds.value.isNotEmpty()

    /** 选中组可释放的总空间（字节） */
    val selectedTotalSize: Long
        get() = groups
            .filter { it.groupId in _selectedGroupIds.value }
            .sumOf { it.size }

    /** 选中组包含的可删除图片总数 */
    val selectedImageCount: Int
        get() = groups
            .filter { it.groupId in _selectedGroupIds.value }
            .sumOf { it.canDeleteImages?.size ?: it.images.size - 1 }

    /**
     * 切换分组选中状态。
     *
     * @param groupId 切换的目标分组 ID
     */
    fun toggleGroupSelection(groupId: Long) {
        _selectedGroupIds.value = if (groupId in _selectedGroupIds.value) {
            _selectedGroupIds.value - groupId
        } else {
            _selectedGroupIds.value + groupId
        }
    }

    /**
     * 全选 / 取消全选。
     */
    fun selectAll() {
        _selectedGroupIds.value = if (isAllSelected) {
            emptySet()
        } else {
            groups.map { it.groupId }.toSet()
        }
    }

    /**
     * 删除所有选中分组中的可删除图片。
     *
     * 收集各组中 [DuplicateGroup.canDeleteImages] 列出的图片，
     * 批量调用 [FileOperator.deleteImages] 执行删除。
     *
     * @return 发射每条删除操作结果的 Flow
     */
    fun deleteSelected(): Flow<DeleteResult> = kotlinx.coroutines.flow.flow {
        val imagesToDelete = groups
            .filter { it.groupId in _selectedGroupIds.value }
            .flatMap { group ->
                group.canDeleteImages?.filter { it.id != group.bestImage?.id }
                    ?: group.images.filter { it.id != group.bestImage?.id }
            }

        if (imagesToDelete.isEmpty()) return@flow

        // 逐张删除并发射结果
        for (image in imagesToDelete) {
            val result = fileOperator.deleteImage(image)
            emit(result)
        }
    }

    /**
     * 清除所有选中状态。
     */
    fun clearSelection() {
        _selectedGroupIds.value = emptySet()
    }
}
