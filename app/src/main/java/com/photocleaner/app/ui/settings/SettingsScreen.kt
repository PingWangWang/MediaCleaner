/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 设置页面 Compose UI
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.photocleaner.feature.settings.ui.SettingsScreen as FeatureSettingsScreen
import com.photocleaner.feature.settings.ui.SettingsViewModel

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    FeatureSettingsScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack
    )
}
