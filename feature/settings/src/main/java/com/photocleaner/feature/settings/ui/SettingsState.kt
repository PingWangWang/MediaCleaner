package com.photocleaner.feature.settings.ui

data class SettingsState(
    val scanConcurrency: Int = 4,
    val highPrecisionEnabled: Boolean = false,
    val autoClearRecycle: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "en",
    val notificationEnabled: Boolean = true,
    val lastScanTime: Long? = null,
    val shakeToFeedback: Boolean = true,
    val privacyLock: Boolean = false,
    val appVersion: String = "1.0.0",
    val appVersionCode: Int = 1
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}
