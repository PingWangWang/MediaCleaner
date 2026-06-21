package com.photocleaner.feature.scanner.model

/**
 * 扫描进度状态密封类。
 *
 * 用于向 UI 层报告图片扫描的各个阶段，
 * 包括开始、进行中（已扫描数量/总数）、完成和错误。
 *
 * @author PhotoCleaner
 */
sealed class ScanProgress {

    /** 扫描已启动 */
    data object STARTED : ScanProgress()

    /** 扫描进行中 */
    data class SCANNING(
        val scannedCount: Int,
        val totalCount: Int
    ) : ScanProgress()

    /** 扫描完成 */
    data class COMPLETED(
        val totalCount: Int,
        val newCount: Int
    ) : ScanProgress()

    /** 扫描出错 */
    data class ERROR(val message: String) : ScanProgress()
}
