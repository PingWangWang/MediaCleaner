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
            "autoClearRecycle" -> settingsPreferences.setAutoClearRecycle(value as Boolean)
            "themeMode" -> settingsPreferences.setThemeMode(value as String)
            "language" -> settingsPreferences.setLanguage(value as String)
            "notificationEnabled" -> settingsPreferences.setNotificationEnabled(value as Boolean)
            "shakeToFeedback" -> settingsPreferences.setShakeToFeedback(value as Boolean)
            "privacyLock" -> settingsPreferences.setPrivacyLock(value as Boolean)
            else -> throw IllegalArgumentException("Unknown settings key: $key")
        }
    }
}
