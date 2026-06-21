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

    fun updateScanConcurrency(value: Int) {
        viewModelScope.launch {
            updateSettingUseCase("scanConcurrency", value)
        }
    }

    fun updateHighPrecision(enabled: Boolean) {
        viewModelScope.launch {
            updateSettingUseCase("highPrecisionEnabled", enabled)
        }
    }

    fun updateAutoClearRecycle(enabled: Boolean) {
        viewModelScope.launch {
            updateSettingUseCase("autoClearRecycle", enabled)
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            updateSettingUseCase("themeMode", mode.name)
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            updateSettingUseCase("language", lang)
        }
    }

    fun updateNotification(enabled: Boolean) {
        viewModelScope.launch {
            updateSettingUseCase("notificationEnabled", enabled)
        }
    }

    fun updateShakeToFeedback(enabled: Boolean) {
        viewModelScope.launch {
            updateSettingUseCase("shakeToFeedback", enabled)
        }
    }

    fun updatePrivacyLock(enabled: Boolean) {
        viewModelScope.launch {
            updateSettingUseCase("privacyLock", enabled)
        }
    }
}
