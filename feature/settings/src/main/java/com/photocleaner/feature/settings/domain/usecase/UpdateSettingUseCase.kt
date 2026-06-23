/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 更新设置项用例
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.settings.domain.usecase

import com.photocleaner.feature.settings.data.SettingsPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class UpdateSettingUseCase @Inject constructor(
    private val settingsPreferences: SettingsPreferences?
) {
    open suspend operator fun invoke(key: String, value: Any) {
        val prefs = requireNotNull(settingsPreferences) {
            "UpdateSettingUseCase: SettingsPreferences must not be null"
        }
        when (key) {
            "scanConcurrency" -> prefs.setScanConcurrency(value as Int)
            "highPrecisionEnabled" -> prefs.setHighPrecisionEnabled(value as Boolean)
            "minImageSizeKb" -> prefs.setMinImageSizeKb(value as Int)
            "ignoreScreenshots" -> prefs.setIgnoreScreenshots(value as Boolean)
            "ignoreGif" -> prefs.setIgnoreGif(value as Boolean)
            "similarityThreshold" -> prefs.setSimilarityThreshold(value as Int)
            "autoRetainBest" -> prefs.setAutoRetainBest(value as Boolean)
            "confirmBeforeDelete" -> prefs.setConfirmBeforeDelete(value as Boolean)
            "recycleBinEnabled" -> prefs.setRecycleBinEnabled(value as Boolean)
            "autoClearRecycle" -> prefs.setAutoClearRecycle(value as Boolean)
            "autoClearRecycleDays" -> prefs.setAutoClearRecycleDays(value as Int)
            "autoCheckUpdate" -> prefs.setAutoCheckUpdate(value as Boolean)
            "scanOnWifiOnly" -> prefs.setScanOnWifiOnly(value as Boolean)
            "themeMode" -> prefs.setThemeMode(value as String)
            "language" -> prefs.setLanguage(value as String)
            "notificationEnabled" -> prefs.setNotificationEnabled(value as Boolean)
            "shakeToFeedback" -> prefs.setShakeToFeedback(value as Boolean)
            "privacyLock" -> prefs.setPrivacyLock(value as Boolean)
            "privacyPolicyAccepted" -> prefs.setPrivacyPolicyAccepted(value as Boolean)
            "deviceTier" -> prefs.setDeviceTier(value as String)
            else -> throw IllegalArgumentException("Unknown settings key: $key")
        }
    }
}
