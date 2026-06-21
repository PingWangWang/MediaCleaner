package com.photocleaner.app.ui.recyclebin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.core.database.entity.RecycleItemEntity
import com.photocleaner.feature.fileops.domain.FileOperator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 回收站页面 ViewModel。
 *
 * 管理回收站列表的加载、恢复、永久删除和清空操作。
 */
@HiltViewModel
class RecycleBinViewModel @Inject constructor(
    private val fileOperator: FileOperator
) : ViewModel() {

    /** 回收站条目列表 */
    private val _items = MutableStateFlow<List<RecycleItemEntity>>(emptyList())
    val items: StateFlow<List<RecycleItemEntity>> = _items.asStateFlow()

    /** 回收站条目数量 */
    private val _itemCount = MutableStateFlow(0)
    val itemCount: StateFlow<Int> = _itemCount.asStateFlow()

    init {
        loadItems()
    }

    /**
     * 从 [FileOperator] 加载回收站条目。
     */
    private fun loadItems() {
        viewModelScope.launch {
            fileOperator.getRecycleBinItems().collect { recycleItems ->
                _items.value = recycleItems
                _itemCount.value = recycleItems.size
            }
        }
    }

    /**
     * 恢复指定回收站条目到原始位置。
     *
     * @param itemId 回收站条目 ID
     */
    fun restoreItem(itemId: Long) {
        viewModelScope.launch {
            try {
                val success = fileOperator.restoreImage(itemId)
                if (success) {
                    // 恢复成功后列表会自动刷新（通过 Flow 观察）
                    _items.value = _items.value.filter { it.id != itemId }
                    _itemCount.value = _items.value.size
                }
            } catch (_: Exception) {
                // 恢复失败，保持列表不变
            }
        }
    }

    /**
     * 永久删除指定回收站条目。
     *
     * @param itemId 回收站条目 ID
     */
    fun permanentlyDelete(itemId: Long) {
        viewModelScope.launch {
            try {
                fileOperator.permanentlyDelete(itemId)
                _items.value = _items.value.filter { it.id != itemId }
                _itemCount.value = _items.value.size
            } catch (_: Exception) {
                // 删除失败
            }
        }
    }

    /**
     * 清空回收站所有条目。
     */
    fun clearAll() {
        viewModelScope.launch {
            try {
                val currentItems = _items.value.toList()
                for (item in currentItems) {
                    fileOperator.permanentlyDelete(item.id)
                }
                _items.value = emptyList()
                _itemCount.value = 0
            } catch (_: Exception) {
                // 清空失败
            }
        }
    }

    /**
     * 清理所有已过期的回收站条目。
     */
    fun clearExpired() {
        viewModelScope.launch {
            try {
                fileOperator.clearExpiredItems()
                // 列表将自动通过 Flow 刷新
            } catch (_: Exception) {
                // 清理失败
            }
        }
    }
}
