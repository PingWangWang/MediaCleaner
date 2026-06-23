/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 扫描页面 ViewModel
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.core.common.model.DuplicateGroup
import com.photocleaner.core.database.dao.ImageDao
import com.photocleaner.core.database.entity.toImageItem
import com.photocleaner.feature.duplicate.domain.usecase.DetectDuplicateUseCase
import com.photocleaner.feature.scanner.domain.usecase.ScanImageUseCase
import com.photocleaner.feature.scanner.model.ScanProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 扫描页面 ViewModel。
 *
 * 分两步执行：
 * 1. [startScan]：仅扫描 MediaStore 图片，不执行去重（用户信息安全优先）
 * 2. [startDetection]：用户手动触发后，从数据库加载图片并执行去重检测
 */
@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanImageUseCase: ScanImageUseCase,
    private val detectDuplicateUseCase: DetectDuplicateUseCase,
    private val imageDao: ImageDao
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    /**
     * 启动扫描流程（仅扫描，不去重）。
     *
     * 扫描完成后进入 [ScanUiState.ScanCompleted] 状态，
     * 由用户手动调用 [startDetection] 触发重复检测。
     */
    fun startScan() {
        if (_scanState.value !is ScanUiState.Idle) return

        viewModelScope.launch {
            _scanState.value = ScanUiState.Starting

            try {
                // Phase 1: 全量扫描图片
                scanImageUseCase().collect { progress ->
                    when (progress) {
                        is ScanProgress.STARTED -> {
                            _scanState.value = ScanUiState.Starting
                        }

                        is ScanProgress.SCANNING -> {
                            val p = if (progress.totalCount > 0) {
                                progress.scannedCount.toFloat() / progress.totalCount.toFloat()
                            } else 0f
                            _scanState.value = ScanUiState.Scanning(
                                progress = p.coerceIn(0f, 1f),
                                phase = "正在扫描图片...",
                                scannedCount = progress.scannedCount,
                                totalCount = progress.totalCount,
                                foundDuplicates = 0
                            )
                        }

                        is ScanProgress.COMPLETED -> {
                            _scanState.value = ScanUiState.Scanning(
                                progress = 1f,
                                phase = "扫描完成，等待检测",
                                scannedCount = progress.totalCount,
                                totalCount = progress.totalCount,
                                foundDuplicates = 0
                            )
                        }

                        is ScanProgress.ERROR -> {
                            _scanState.value = ScanUiState.Error(progress.message)
                            return@collect
                        }
                    }
                }
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(
                    "扫描过程异常：${e.localizedMessage ?: "未知错误"}"
                )
                return@launch
            }

            // 扫描完成，进入待检测状态，需要用户手动确认才执行去重
            val totalCount = when (val state = _scanState.value) {
                is ScanUiState.Scanning -> state.totalCount
                else -> 0
            }
            _scanState.value = ScanUiState.ScanCompleted(totalCount = totalCount)
        }
    }

    /**
     * 手动触发重复检测。
     *
     * 用户确认后从数据库加载图片并执行去重算法，
     * 完成后进入 [ScanUiState.Complete] 状态（含分组结果）。
     */
    fun startDetection() {
        if (_scanState.value !is ScanUiState.ScanCompleted) return
        val scanCompleted = _scanState.value as ScanUiState.ScanCompleted

        viewModelScope.launch {
            _scanState.value = ScanUiState.Scanning(
                progress = 1f,
                phase = "正在检测重复图片...",
                scannedCount = scanCompleted.totalCount,
                totalCount = scanCompleted.totalCount,
                foundDuplicates = 0
            )

            // Phase 2: 从数据库加载所有图片
            val images = try {
                imageDao.getAll().map { it.toImageItem() }
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(
                    "加载图片数据失败：${e.localizedMessage ?: "未知错误"}"
                )
                return@launch
            }

            if (images.isEmpty()) {
                _scanState.value = ScanUiState.Complete(emptyList())
                return@launch
            }

            // Phase 3: 重复检测，收集分组结果
            val allGroups = mutableListOf<DuplicateGroup>()
            var foundCount = 0
            val totalImages = images.size

            try {
                detectDuplicateUseCase(images).collect { group ->
                    allGroups.add(group)
                    foundCount++
                    _scanState.value = ScanUiState.Scanning(
                        progress = 1f,
                        phase = "正在整理结果...",
                        scannedCount = totalImages,
                        totalCount = totalImages,
                        foundDuplicates = foundCount
                    )
                }
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(
                    "重复检测失败：${e.localizedMessage ?: "未知错误"}"
                )
                return@launch
            }

            _scanState.value = ScanUiState.Complete(allGroups)
        }
    }

    /**
     * 重置扫描状态为 Idle，允许重新扫描。
     */
    fun resetState() {
        _scanState.value = ScanUiState.Idle
    }
}

/**
 * 扫描页面 UI 状态密封类。
 */
sealed class ScanUiState {

    /** 初始闲置状态 */
    data object Idle : ScanUiState()

    /** 扫描已启动（准备阶段） */
    data object Starting : ScanUiState()

    /** 扫描或检测进行中 */
    data class Scanning(
        val progress: Float,
        val phase: String,
        val scannedCount: Int,
        val totalCount: Int,
        val foundDuplicates: Int
    ) : ScanUiState()

    /** 扫描已完成，等待用户确认执行重复检测 */
    data class ScanCompleted(
        val totalCount: Int
    ) : ScanUiState()

    /** 扫描和检测已完成，返回分组结果 */
    data class Complete(
        val groups: List<DuplicateGroup>
    ) : ScanUiState()

    /** 扫描或检测过程出错 */
    data class Error(
        val message: String
    ) : ScanUiState()
}
