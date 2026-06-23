/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 应用唯一 Activity 宿主，Compose 入口
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.photocleaner.app.navigation.AppNavGraph
import com.photocleaner.app.ui.theme.PhotoCleanerTheme
import com.photocleaner.feature.settings.data.SettingsPreferences
import com.photocleaner.feature.settings.ui.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 清图大师主 Activity。
 *
 * 使用 Hilt 注解 @AndroidEntryPoint 启用依赖注入，
 * 在 onCreate 中设置边缘到边缘显示并加载 Compose 界面。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsPreferences: SettingsPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsPreferences.themeMode
                .collectAsState(initial = ThemeMode.SYSTEM.name)
            val isDarkTheme = when (ThemeMode.valueOf(themeMode)) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            PhotoCleanerTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val agreementAccepted by settingsPreferences.privacyPolicyAccepted
                        .collectAsState(initial = null)
                    val scope = rememberCoroutineScope()
                    AppNavGraph(
                        agreementAccepted = agreementAccepted,
                        onAcceptAgreement = {
                            scope.launch {
                                settingsPreferences.setPrivacyPolicyAccepted(true)
                            }
                        }
                    )
                }
            }
        }
    }
}
