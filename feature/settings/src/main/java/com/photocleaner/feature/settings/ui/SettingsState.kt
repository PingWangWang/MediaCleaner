package com.photocleaner.feature.settings.ui

data class SettingsState(
    // ── 扫描设置 ─────────────────────────────────────────────
    val scanConcurrency: Int = 4,
    val highPrecisionEnabled: Boolean = false,
    val minImageSizeKb: Int = 100,
    val ignoreScreenshots: Boolean = false,
    val ignoreGif: Boolean = true,

    // ── 去重设置 ─────────────────────────────────────────────
    val similarityThreshold: Int = 10,
    val autoRetainBest: Boolean = true,

    // ── 删除设置 ─────────────────────────────────────────────
    val confirmBeforeDelete: Boolean = true,
    val recycleBinEnabled: Boolean = true,
    val autoClearRecycle: Boolean = false,
    val autoClearRecycleDays: Int = 30,

    // ── 升级设置 ─────────────────────────────────────────────
    val autoCheckUpdate: Boolean = true,
    val scanOnWifiOnly: Boolean = true,

    // ── 通用设置 ─────────────────────────────────────────────
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: String = "en",
    val notificationEnabled: Boolean = true,
    val shakeToFeedback: Boolean = true,

    // ── 隐私 ─────────────────────────────────────────────────
    val privacyLock: Boolean = false,
    val privacyPolicyAccepted: Boolean = false,

    // ── 设备 ─────────────────────────────────────────────────
    val deviceTier: String = "MEDIUM",

    // ── 应用信息 ─────────────────────────────────────────────
    val lastScanTime: Long? = null,
    val appVersion: String = "1.0.0",
    val appVersionCode: Int = 1
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}
