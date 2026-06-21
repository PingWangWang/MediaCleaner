package com.photocleaner.feature.settings.domain.usecase

import com.photocleaner.feature.settings.data.SettingsPreferences
import com.photocleaner.feature.settings.ui.SettingsState
import com.photocleaner.feature.settings.ui.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetSettingsUseCase @Inject constructor(
    private val settingsPreferences: SettingsPreferences
) {
    operator fun invoke(): Flow<SettingsState> = combine(
        settingsPreferences.scanConcurrency,
        settingsPreferences.highPrecisionEnabled,
        settingsPreferences.autoClearRecycle,
        settingsPreferences.themeMode,
        settingsPreferences.language,
        settingsPreferences.notificationEnabled,
        settingsPreferences.lastScanTime,
        settingsPreferences.shakeToFeedback,
        settingsPreferences.privacyLock
    ) { args: Array<*> ->
        SettingsState(
            scanConcurrency = args[0] as Int,
            highPrecisionEnabled = args[1] as Boolean,
            autoClearRecycle = args[2] as Boolean,
            themeMode = ThemeMode.valueOf(args[3] as String),
            language = args[4] as String,
            notificationEnabled = args[5] as Boolean,
            lastScanTime = args[6] as Long?,
            shakeToFeedback = args[7] as Boolean,
            privacyLock = args[8] as Boolean
        )
    }
}
