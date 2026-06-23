/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 扫描结果共享单例，跨页面传递扫描数据
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app.navigation

import com.photocleaner.core.common.model.DuplicateGroup

/**
 * 扫描结果持有者（全局单例）。
 *
 * 用于在导航过程中跨 Screen 传递扫描结果，避免序列化大量数据到 Navigation
 * argument 中。各 Screen 在进入时从该单例读取所需数据，退出时无需清理。
 *
 * @property groups        最近一次扫描得到的重复分组列表
 * @property selectedGroup 用户在结果页点击后选中的单个分组（供详情页使用）
 */
object ScanResultHolder {
    var groups: List<DuplicateGroup> = emptyList()
    var selectedGroup: DuplicateGroup? = null

    /** 重置所有状态，释放引用。推荐在扫描开始时调用。 */
    fun clear() {
        groups = emptyList()
        selectedGroup = null
    }
}
