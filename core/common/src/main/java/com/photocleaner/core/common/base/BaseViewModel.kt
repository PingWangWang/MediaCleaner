package com.photocleaner.core.common.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope

/**
 * 所有 ViewModel 的抽象基类。
 *
 * 提供统一的 [viewModelScope] 访问入口，子类可基于此发起协程任务。
 * 覆写 [onCleared] 时请记得调用 `super.onCleared()` 以释放公共资源。
 *
 * @author PhotoCleaner
 */
abstract class BaseViewModel : ViewModel() {

    /**
     * 受保护的协程作用域，绑定至 ViewModel 生命周期。
     * 当 ViewModel 被清除时该作用域会自动取消，避免协程泄漏。
     */
    protected val scope: CoroutineScope
        get() = viewModelScope

    /**
     * ViewModel 清理回调。
     *
     * 子类可覆写此方法以释放自身持有的资源（如关闭文件流、取消网络请求等）。
     * 始终调用 `super.onCleared()` 以确保父类行为正常执行。
     */
    override fun onCleared() {
        super.onCleared()
    }
}
