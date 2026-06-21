package com.photocleaner.core.common.constant

/**
 * 应用全局常量。
 *
 * 定义数据库配置、扫描并发参数、回收站自动清理天数等通用常量。
 *
 * @author PhotoCleaner
 */
object AppConstants {

    /** Room 数据库名称 */
    const val DB_NAME: String = "photocleaner_db"

    /** Room 数据库版本号 */
    const val DB_VERSION: Int = 1

    /** 每次分块扫描的图片数量 */
    const val SCAN_CHUNK_SIZE: Int = 500

    /** 高并发扫描线程数 */
    const val SCAN_CONCURRENCY_HIGH: Int = 6

    /** 中等并发扫描线程数 */
    const val SCAN_CONCURRENCY_MEDIUM: Int = 4

    /** 低并发扫描线程数 */
    const val SCAN_CONCURRENCY_LOW: Int = 2

    /** 回收站自动清理天数：30 天 */
    const val RECYCLE_AUTO_CLEAR_DAYS: Int = 30
}
