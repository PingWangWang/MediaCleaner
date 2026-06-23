package com.photocleaner.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.core.common.model.DuplicateGroup
import com.photocleaner.core.database.dao.ImageDao
import com.photocleaner.core.database.entity.toImageItem
import com.photocleaner.feature.duplicate.domain.usecase.DetectDuplicateUseCase
import com.photocleaner.feature.fileops.domain.FileOperator
import com.photocleaner.feature.fileops.model.DeleteResult
import com.photocleaner.feature.scanner.domain.usecase.ScanImageUseCase
import com.photocleaner.feature.scanner.model.ScanProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HomeUiState {
    data object Idle : HomeUiState()
    data object Starting : HomeUiState()
    data class Scanning(val progress: Float, val scannedCount: Int, val totalCount: Int) : HomeUiState()
    data class ScanCompleted(val totalCount: Int) : HomeUiState()
    data class Detecting(val foundGroups: Int) : HomeUiState()
    data class Complete(val groups: List<DuplicateGroup>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scanImageUseCase: ScanImageUseCase,
    private val detectDuplicateUseCase: DetectDuplicateUseCase,
    private val imageDao: ImageDao,
    private val fileOperator: FileOperator
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _selectedGroupIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedGroupIds: StateFlow<Set<Long>> = _selectedGroupIds.asStateFlow()

    fun startScan() {
        if (_state.value !is HomeUiState.Idle) return
        viewModelScope.launch {
            _state.value = HomeUiState.Starting
            try {
                scanImageUseCase().collect { progress ->
                    when (progress) {
                        is ScanProgress.STARTED -> _state.value = HomeUiState.Starting
                        is ScanProgress.SCANNING -> {
                            val p = if (progress.totalCount > 0) progress.scannedCount.toFloat() / progress.totalCount else 0f
                            _state.value = HomeUiState.Scanning(p, progress.scannedCount, progress.totalCount)
                        }
                        is ScanProgress.COMPLETED -> {}
                        is ScanProgress.ERROR -> { _state.value = HomeUiState.Error(progress.message); return@collect }
                    }
                }
                val total = when (val s = _state.value) { is HomeUiState.Scanning -> s.totalCount else -> 0 }
                _state.value = HomeUiState.ScanCompleted(total)
            } catch (e: Exception) {
                _state.value = HomeUiState.Error("扫描异常：${e.localizedMessage ?: "未知错误"}")
            }
        }
    }

    fun startDetection() {
        if (_state.value !is HomeUiState.ScanCompleted) return
        val completed = _state.value as HomeUiState.ScanCompleted
        viewModelScope.launch {
            _state.value = HomeUiState.Detecting(0)
            val images = try {
                imageDao.getAll().map { it.toImageItem() }
            } catch (e: Exception) {
                _state.value = HomeUiState.Error("加载图片失败：${e.localizedMessage ?: "未知错误"}")
                return@launch
            }
            if (images.isEmpty()) { _state.value = HomeUiState.Complete(emptyList()); return@launch }
            val groups = mutableListOf<DuplicateGroup>()
            try {
                detectDuplicateUseCase(images).collect { group ->
                    groups.add(group)
                    _state.value = HomeUiState.Detecting(groups.size)
                }
            } catch (e: Exception) {
                _state.value = HomeUiState.Error("检测失败：${e.localizedMessage ?: "未知错误"}")
                return@launch
            }
            _state.value = HomeUiState.Complete(groups)
        }
    }

    fun toggleGroupSelection(groupId: Long) {
        _selectedGroupIds.value = if (groupId in _selectedGroupIds.value)
            _selectedGroupIds.value - groupId else _selectedGroupIds.value + groupId
    }

    fun selectAll() {
        val groups = (_state.value as? HomeUiState.Complete)?.groups ?: return
        _selectedGroupIds.value = if (_selectedGroupIds.value.size < groups.size)
            groups.map { it.groupId }.toSet() else emptySet()
    }

    fun clearSelection() { _selectedGroupIds.value = emptySet() }

    fun deleteSelected(): Flow<DeleteResult> = flow {
        val groups = (_state.value as? HomeUiState.Complete)?.groups ?: return@flow
        val ids = _selectedGroupIds.value
        for (group in groups.filter { it.groupId in ids }) {
            for (image in group.images) {
                emit(fileOperator.deleteImage(image))
            }
        }
    }
}
