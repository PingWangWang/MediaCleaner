package com.photocleaner.feature.scanner.domain.usecase

import com.photocleaner.core.common.constant.AppConstants
import com.photocleaner.core.common.model.ImageItem
import com.photocleaner.feature.scanner.domain.repository.ImageRepository
import com.photocleaner.feature.scanner.model.ScanProgress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import com.photocleaner.core.common.utils.LoggingManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 增量扫描用例。
 *
 * 仅扫描自上次扫描后修改的图片，计算分桶并保存到数据库。
 * 适用于快速刷新而非全量重新扫描。
 *
 * @author PhotoCleaner
 */
@Singleton
class IncrementalScanUseCase @Inject constructor(
    private val imageRepository: ImageRepository
) {

    companion object {
        private const val TAG = "IncrementalScanUseCase"
    }

    /**
     * 执行增量图片扫描。
     *
     * @param lastScanTime 上次扫描的时间戳（毫秒），仅扫描此时间之后修改的图片
     * @return Flow 发射扫描进度状态
     */
    operator fun invoke(lastScanTime: Long): Flow<ScanProgress> = flow {
        LoggingManager.d(TAG, "Incremental scan started since: $lastScanTime")

        // 1. 发射 STARTED 状态
        emit(ScanProgress.STARTED)

        var scannedCount = 0

        try {
            // 2. 从 MediaStore 扫描增量图片
            val imageFlow = imageRepository.scanIncremental(lastScanTime)

            val tempList = mutableListOf<ImageItem>()
            imageFlow.collect { imageItem ->
                tempList.add(imageItem)
            }

            val totalCount = tempList.size
            LoggingManager.d(TAG, "New/modified images found: $totalCount")

            if (totalCount == 0) {
                emit(ScanProgress.COMPLETED(totalCount = 0, newCount = 0))
                return@flow
            }

            // 3. 分批处理：计算分桶并保存
            val processedImages = mutableListOf<ImageItem>()

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

                // 每处理一批就发射进度并保存
                if (processedImages.size >= AppConstants.SCAN_CHUNK_SIZE ||
                    index == tempList.lastIndex
                ) {
                    emit(ScanProgress.SCANNING(scannedCount, totalCount))
                    saveBatch(processedImages)
                    processedImages.clear()
                }
            }

            // 4. 保存剩余的图片
            if (processedImages.isNotEmpty()) {
                saveBatch(processedImages)
            }

            LoggingManager.d(TAG, "Incremental scan completed: $totalCount new images")
            emit(ScanProgress.COMPLETED(totalCount = totalCount, newCount = totalCount))

        } catch (e: Exception) {
            LoggingManager.e(TAG, "Incremental scan failed after scanning $scannedCount images: ${e.message}")
            LoggingManager.e(TAG, e.stackTraceToString())
            emit(ScanProgress.ERROR(e.message ?: "Unknown error during incremental scan"))
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
     * 基于文件大小的 log2 值进行指数分桶。
     */
    private fun computeSizeBucket(size: Long): Int {
        if (size <= 0) return 0
        return (63 - java.lang.Long.numberOfLeadingZeros(size)).coerceAtLeast(0)
    }

    /**
     * 计算宽高比分桶。
     *
     * 将宽高比离散化为有限数量的桶：
     * - 0: 未知
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
            ratio in 0.8f..1.2f -> 1
            ratio in 1.2f..1.8f -> 2
            ratio > 1.8f -> 3
            ratio in 0.5f..0.8f -> 4
            else -> 5
        }
    }
}
