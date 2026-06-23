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
class UpdateSettingUseCase @Inject constructor(
    private val settingsPreferences: SettingsPreferences
) {
    suspend operator fun invoke(key: String, value: Any) {
        when (key) {
            "scanConcurrency" -> settingsPreferences.setScanConcurrency(value as Int)
            "highPrecisionEnabled" -> settingsPreferences.setHighPrecisionEnabled(value as Boolean)
            "minImageSizeKb" -> settingsPreferences.setMinImageSizeKb(value as Int)
            "ignoreScreenshots" -> settingsPreferences.setIgnoreScreenshots(value as Boolean)
            "ignoreGif" -> settingsPreferences.setIgnoreGif(value as Boolean)
            "similarityThreshold" -> settingsPreferences.setSimilarityThreshold(value as Int)
            "autoRetainBest" -> settingsPreferences.setAutoRetainBest(value as Boolean)
            "confirmBeforeDelete" -> settingsPreferences.setConfirmBeforeDelete(value as Boolean)
            "recycleBinEnabled" -> settingsPreferences.setRecycleBinEnabled(value as Boolean)
            "autoClearRecycle" -> settingsPreferences.setAutoClearRecycle(value as Boolean)
            "autoClearRecycleDays" -> settingsPreferences.setAutoClearRecycleDays(value as Int)
            "autoCheckUpdate" -> settingsPreferences.setAutoCheckUpdate(value as Boolean)
            "scanOnWifiOnly" -> settingsPreferences.setScanOnWifiOnly(value as Boolean)
            "themeMode" -> settingsPreferences.setThemeMode(value as String)
            "language" -> settingsPreferences.setLanguage(value as String)
            "notificationEnabled" -> settingsPreferences.setNotificationEnabled(value as Boolean)
            "shakeToFeedback" -> settingsPreferences.setShakeToFeedback(value as Boolean)
            "privacyLock" -> settingsPreferences.setPrivacyLock(value as Boolean)
            "privacyPolicyAccepted" -> settingsPreferences.setPrivacyPolicyAccepted(value as Boolean)
            "deviceTier" -> settingsPreferences.setDeviceTier(value as String)
            else -> throw IllegalArgumentException("Unknown settings key: $key")
        }
    }
}
