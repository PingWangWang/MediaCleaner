/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 重复检测器抽象基类
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.duplicate.base

import com.photocleaner.core.common.model.DuplicateGroup
import com.photocleaner.core.common.model.ImageItem
import kotlinx.coroutines.flow.Flow

/**
 * 重复图片检测器接口。
 *
 * 定义检测重复图片的两种模式：
 * - [detect]：对给定的图片列表执行完整检测
 * - [detectIncremental]：增量检测（适用于持续扫描场景）
 *
 * 两者均以 [Flow] 形式逐个发射 [DuplicateGroup]，支持流式消费。
 *
 * @author PhotoCleaner
 */
interface DuplicateDetector {

    /**
     * 对给定的图片列表执行重复检测。
     *
     * @param images 待检测的图片列表
     * @return 发射 [DuplicateGroup] 的流，每个元素代表一组重复图片
     */
    suspend fun detect(images: List<ImageItem>): Flow<DuplicateGroup>

    /**
     * 执行增量重复检测（基于已有缓存或上次扫描结果）。
     *
     * 适用于图片库发生局部变更（新增/删除）时仅计算差异部分的场景。
     *
     * @return 发射 [DuplicateGroup] 的流，每个元素代表一组重复图片
     */
    suspend fun detectIncremental(): Flow<DuplicateGroup>
}
