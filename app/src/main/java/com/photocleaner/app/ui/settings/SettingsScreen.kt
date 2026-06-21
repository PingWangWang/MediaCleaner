package com.photocleaner.app.ui.settings

import androidx.compose.runtime.Composable
import com.photocleaner.feature.settings.ui.SettingsScreen as FeatureSettingsScreen

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    FeatureSettingsScreen(onNavigateBack = onNavigateBack)
}
