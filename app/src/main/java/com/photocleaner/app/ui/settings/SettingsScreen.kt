/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 设置页面 Compose UI
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app.ui.settings

import androidx.compose.runtime.Composable
import com.photocleaner.feature.settings.ui.SettingsScreen as FeatureSettingsScreen

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    FeatureSettingsScreen(onNavigateBack = onNavigateBack)
}
