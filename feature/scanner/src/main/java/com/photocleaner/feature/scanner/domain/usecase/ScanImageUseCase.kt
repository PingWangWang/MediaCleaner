/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 全量图片扫描用例
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.scanner.domain.usecase

import android.content.Context
import android.util.Log
import com.photocleaner.core.common.constant.AppConstants
import com.photocleaner.core.common.model.ImageItem
import com.photocleaner.core.common.utils.DeviceClassifier
import com.photocleaner.feature.scanner.domain.repository.ImageRepository
import com.photocleaner.feature.scanner.model.ScanProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全量扫描用例。
 *
 * 扫描 MediaStore 中的所有图片，计算大小分桶和宽高比分桶，
 * 将结果保存到本地数据库，并通过 [ScanProgress] 向 UI 层报告进度。
 *
 * 自动检测设备性能等级并据此调整扫描并发数。
 *
 * @author PhotoCleaner
 */
@Singleton
open class ScanImageUseCase @Inject constructor(
    private val imageRepository: ImageRepository,
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "ScanImageUseCase"
    }

    /**
     * 执行全量图片扫描。
     *
     * @return Flow 发射扫描进度状态
     */
    open operator fun invoke(): Flow<ScanProgress> = flow {
        // ── 1. 设备分级 ────────────────────────────────────────────
        val deviceTier = DeviceClassifier.classify(context)
        val recommendedConcurrency = DeviceClassifier.getRecommendedConcurrency(deviceTier)
        val algorithmLevel = DeviceClassifier.getRecommendedAlgorithmLevel(deviceTier)

        Log.d(TAG, "Device classified: tier=$deviceTier, concurrency=$recommendedConcurrency, " +
                "algorithm=$algorithmLevel")
        Log.d(TAG, "Scan started")

        // 2. 发射 STARTED 状态
        emit(ScanProgress.STARTED)

        var scannedCount = 0

        try {
            // 3. 从 MediaStore 扫描所有图片
            val imageFlow = imageRepository.scanAllImages()

            // 收集所有图片到列表以便计算总数
            val tempList = mutableListOf<ImageItem>()
            imageFlow.collect { imageItem ->
                tempList.add(imageItem)
            }

            val totalCount = tempList.size
            Log.d(TAG, "Total images found: $totalCount")

            // 4. 分批处理：计算分桶并保存，使用设备推荐并发策略
            val processedImages = mutableListOf<ImageItem>()

            // 根据设备等级决定每次处理的批大小
            val batchSize = AppConstants.SCAN_CHUNK_SIZE.coerceAtMost(
                recommendedConcurrency * 100
            )

            for ((index, image) in tempList.withIndex()) {
                // 计算大小分桶 (log2)
                val sizeBucket = computeSizeBucket(image.size)
                // 计算宽高比分桶
                val ratioBucket = computeRatioBucket(image.width, image.height)

                val processedImage = image.copy(
                    sizeBucket = sizeBucket,
                    ratioBucket = ratioBucket
                )
                processedImages.add(processedImage)
                scannedCount++

                // 每处理一批就发射进度并保存（根据设备调整批次大小）
                if (processedImages.size >= batchSize ||
                    index == tempList.lastIndex
                ) {
                    emit(ScanProgress.SCANNING(scannedCount, totalCount))
                    saveBatch(processedImages)
                    processedImages.clear()
                }
            }

            // 5. 保存剩余的图片
            if (processedImages.isNotEmpty()) {
                saveBatch(processedImages)
            }

            Log.d(TAG, "Scan completed: $totalCount images (tier=$deviceTier, " +
                    "concurrency=$recommendedConcurrency)")
            emit(ScanProgress.COMPLETED(totalCount = totalCount, newCount = totalCount))

        } catch (e: Exception) {
            Log.e(TAG, "Scan failed after scanning $scannedCount images", e)
            emit(ScanProgress.ERROR(e.message ?: "Unknown error during scan"))
        }
    }.flowOn(Dispatchers.Default)

    /**
     * 批量保存图片到数据库。
     */
    private suspend fun saveBatch(images: List<ImageItem>) = withContext(Dispatchers.IO) {
        imageRepository.saveImages(images)
    }

    /**
     * 计算大小分桶。
     *
     * 基于文件大小的 log2 值进行指数分桶，
     * 使相近尺寸的图片落入同一桶中。
     */
    private fun computeSizeBucket(size: Long): Int {
        if (size <= 0) return 0
        return (63 - java.lang.Long.numberOfLeadingZeros(size)).coerceAtLeast(0)
    }

    /**
     * 计算宽高比分桶。
     *
     * 将宽高比离散化为有限数量的桶：
     * - 0: 未知（宽或高为 null / 0）
     * - 1: 近似正方形 (0.8 ~ 1.2)
     * - 2: 横向 (1.2 ~ 1.8)
     * - 3: 超宽 (> 1.8)
     * - 4: 纵向 (0.5 ~ 0.8)
     * - 5: 超高 (< 0.5)
     */
    private fun computeRatioBucket(width: Int?, height: Int?): Int {
        if (width == null || height == null || width == 0 || height == 0) return 0

        val ratio = width.toFloat() / height.toFloat()

        return when {
            ratio in 0.8f..1.2f -> 1  // 近似正方形
            ratio in 1.2f..1.8f -> 2  // 横向
            ratio > 1.8f -> 3         // 超宽
            ratio in 0.5f..0.8f -> 4  // 纵向
            else -> 5                 // 超高
        }
    }
}
