/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 设置页面 Compose UI
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app.ui.settings

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.photocleaner.app.utils.PrivacyPolicyDialog
import com.photocleaner.app.utils.TermsOfServiceDialog
import com.photocleaner.feature.settings.ui.SettingsScreen as FeatureSettingsScreen
import com.photocleaner.feature.settings.ui.SettingsViewModel

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    var showPrivacy by remember { mutableStateOf(false) }
    var showTerms by remember { mutableStateOf(false) }

    if (showPrivacy) {
        PrivacyPolicyDialog(onDismiss = { showPrivacy = false })
    }
    if (showTerms) {
        TermsOfServiceDialog(onDismiss = { showTerms = false })
    }

    FeatureSettingsScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
        onShowPrivacyPolicy = { showPrivacy = true },
        onShowTermsOfService = { showTerms = true }
    )
}
