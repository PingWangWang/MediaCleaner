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
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 全量扫描用例。
 *
 * 扫描 MediaStore 中的所有图片，计算大小分桶和宽高比分桶，
 * 将结果保存到本地数据库，并通过 [ScanProgress] 向 UI 层报告进度。
 *
 * @author PhotoCleaner
 */
@Singleton
class ScanImageUseCase @Inject constructor(
    private val imageRepository: ImageRepository
) {

    companion object {
        private const val TAG = "ScanImageUseCase"
    }

    /**
     * 执行全量图片扫描。
     *
     * @return Flow 发射扫描进度状态
     */
    operator fun invoke(): Flow<ScanProgress> = flow {
        Timber.tag(TAG).d("Scan started")

        // 1. 发射 STARTED 状态
        emit(ScanProgress.STARTED)

        val allImages = mutableListOf<ImageItem>()
        var scannedCount = 0

        try {
            // 2. 从 MediaStore 扫描所有图片
            val imageFlow = imageRepository.scanAllImages()

            // 收集所有图片到列表以便计算总数
            val tempList = mutableListOf<ImageItem>()
            imageFlow.collect { imageItem ->
                tempList.add(imageItem)
            }

            val totalCount = tempList.size
            Timber.tag(TAG).d("Total images found: $totalCount")

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

            Timber.tag(TAG).d("Scan completed: $totalCount images")
            emit(ScanProgress.COMPLETED(totalCount = totalCount, newCount = totalCount))

        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Scan failed after scanning $scannedCount images")
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
