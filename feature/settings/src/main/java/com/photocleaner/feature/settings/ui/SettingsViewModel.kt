package com.photocleaner.feature.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photocleaner.feature.settings.domain.usecase.GetSettingsUseCase
import com.photocleaner.feature.settings.domain.usecase.UpdateSettingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingUseCase: UpdateSettingUseCase
) : ViewModel() {

    val uiState: StateFlow<SettingsState> = getSettingsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsState()
        )

    // ── 扫描设置 ─────────────────────────────────────────────
    fun updateScanConcurrency(value: Int) {
        viewModelScope.launch { updateSettingUseCase("scanConcurrency", value) }
    }

    fun updateHighPrecision(enabled: Boolean) {
        viewModelScope.launch { updateSettingUseCase("highPrecisionEnabled", enabled) }
    }

    fun updateMinImageSizeKb(value: Int) {
        viewModelScope.launch { updateSettingUseCase("minImageSizeKb", value) }
    }

    fun updateIgnoreScreenshots(enabled: Boolean) {
        viewModelScope.launch { updateSettingUseCase("ignoreScreenshots", enabled) }
    }

    fun updateIgnoreGif(enabled: Boolean) {
        viewModelScope.launch { updateSettingUseCase("ignoreGif", enabled) }
    }

    // ── 去重设置 ─────────────────────────────────────────────
    fun updateSimilarityThreshold(value: Int) {
        viewModelScope.launch { updateSettingUseCase("similarityThreshold", value) }
    }

    fun updateAutoRetainBest(enabled: Boolean) {
        viewModelScope.launch { updateSettingUseCase("autoRetainBest", enabled) }
    }

    // ── 删除设置 ─────────────────────────────────────────────
    fun updateConfirmBeforeDelete(enabled: Boolean) {
        viewModelScope.launch { updateSettingUseCase("confirmBeforeDelete", enabled) }
    }

    fun updateRecycleBinEnabled(enabled: Boolean) {
        viewModelScope.launch { updateSettingUseCase("recycleBinEnabled", enabled) }
    }

    fun updateAutoClearRecycle(enabled: Boolean) {
        viewModelScope.launch { updateSettingUseCase("autoClearRecycle", enabled) }
    }

    fun updateAutoClearRecycleDays(days: Int) {
        viewModelScope.launch { updateSettingUseCase("autoClearRecycleDays", days) }
    }

    // ── 升级设置 ─────────────────────────────────────────────
    fun updateAutoCheckUpdate(enabled: Boolean) {
        viewModelScope.launch { updateSettingUseCase("autoCheckUpdate", enabled) }
    }

    fun updateScanOnWifiOnly(enabled: Boolean) {
        viewModelScope.launch { updateSettingUseCase("scanOnWifiOnly", enabled) }
    }

    // ── 通用设置 ─────────────────────────────────────────────
    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { updateSettingUseCase("themeMode", mode.name) }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch { updateSettingUseCase("language", lang) }
    }

    fun updateNotification(enabled: Boolean) {
        viewModelScope.launch { updateSettingUseCase("notificationEnabled", enabled) }
    }

    fun updateShakeToFeedback(enabled: Boolean) {
        viewModelScope.launch { updateSettingUseCase("shakeToFeedback", enabled) }
    }

    // ── 隐私 ─────────────────────────────────────────────────
    fun updatePrivacyLock(enabled: Boolean) {
        viewModelScope.launch { updateSettingUseCase("privacyLock", enabled) }
    }

    fun updatePrivacyPolicyAccepted(accepted: Boolean) {
        viewModelScope.launch { updateSettingUseCase("privacyPolicyAccepted", accepted) }
    }
}
