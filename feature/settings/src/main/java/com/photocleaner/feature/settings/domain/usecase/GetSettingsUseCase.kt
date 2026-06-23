/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 获取设置项用例
 *
 * @author PhotoCleaner
 */
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
        settingsPreferences.minImageSizeKb,
        settingsPreferences.ignoreScreenshots,
        settingsPreferences.ignoreGif,
        settingsPreferences.similarityThreshold,
        settingsPreferences.autoRetainBest,
        settingsPreferences.confirmBeforeDelete,
        settingsPreferences.recycleBinEnabled,
        settingsPreferences.autoClearRecycle,
        settingsPreferences.autoClearRecycleDays,
        settingsPreferences.autoCheckUpdate,
        settingsPreferences.scanOnWifiOnly,
        settingsPreferences.themeMode,
        settingsPreferences.language,
        settingsPreferences.notificationEnabled,
        settingsPreferences.lastScanTime,
        settingsPreferences.shakeToFeedback,
        settingsPreferences.privacyLock,
        settingsPreferences.privacyPolicyAccepted,
        settingsPreferences.deviceTier
    ) { args: Array<*> ->
        SettingsState(
            scanConcurrency = args[0] as Int,
            highPrecisionEnabled = args[1] as Boolean,
            minImageSizeKb = args[2] as Int,
            ignoreScreenshots = args[3] as Boolean,
            ignoreGif = args[4] as Boolean,
            similarityThreshold = args[5] as Int,
            autoRetainBest = args[6] as Boolean,
            confirmBeforeDelete = args[7] as Boolean,
            recycleBinEnabled = args[8] as Boolean,
            autoClearRecycle = args[9] as Boolean,
            autoClearRecycleDays = args[10] as Int,
            autoCheckUpdate = args[11] as Boolean,
            scanOnWifiOnly = args[12] as Boolean,
            themeMode = ThemeMode.valueOf(args[13] as String),
            language = args[14] as String,
            notificationEnabled = args[15] as Boolean,
            lastScanTime = args[16] as Long?,
            shakeToFeedback = args[17] as Boolean,
            privacyLock = args[18] as Boolean,
            privacyPolicyAccepted = args[19] as Boolean,
            deviceTier = args[20] as String
        )
    }
}
