/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 更新类型枚举（可选/强制）
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.appupdate.model

/**
 * 更新类型枚举
 *
 * FORCED: 强制更新，用户必须更新才能继续使用
 * OPTIONAL: 可选更新，用户可以选择忽略
 * NO_UPDATE: 没有可用更新
 */
enum class UpdateType {
    FORCED,
    OPTIONAL,
    NO_UPDATE
}
