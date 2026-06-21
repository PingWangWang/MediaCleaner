package com.photocleaner.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "photo_cleaner_settings")

@Singleton
class SettingsPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ── Preference Keys ────────────────────────────────────────────────────
    private object Keys {
        val SCAN_CONCURRENCY = intPreferencesKey("scan_concurrency")
        val HIGH_PRECISION_ENABLED = booleanPreferencesKey("high_precision_enabled")
        val AUTO_CLEAR_RECYCLE = booleanPreferencesKey("auto_clear_recycle")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val LAST_SCAN_TIME = longPreferencesKey("last_scan_time")
        val SHAKE_TO_FEEDBACK = booleanPreferencesKey("shake_to_feedback")
        val PRIVACY_LOCK = booleanPreferencesKey("privacy_lock")
    }

    // ── Defaults ───────────────────────────────────────────────────────────
    object Defaults {
        const val SCAN_CONCURRENCY = 4
        const val HIGH_PRECISION_ENABLED = false
        const val AUTO_CLEAR_RECYCLE = false
        const val THEME_MODE = "SYSTEM"
        const val LANGUAGE = "en"
        const val NOTIFICATION_ENABLED = true
        const val LAST_SCAN_TIME: Long? = null
        const val SHAKE_TO_FEEDBACK = true
        const val PRIVACY_LOCK = false
    }

    // ── Flow-based Getters ─────────────────────────────────────────────────
    val scanConcurrency: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.SCAN_CONCURRENCY] ?: Defaults.SCAN_CONCURRENCY
    }

    val highPrecisionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HIGH_PRECISION_ENABLED] ?: Defaults.HIGH_PRECISION_ENABLED
    }

    val autoClearRecycle: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_CLEAR_RECYCLE] ?: Defaults.AUTO_CLEAR_RECYCLE
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: Defaults.THEME_MODE
    }

    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.LANGUAGE] ?: Defaults.LANGUAGE
    }

    val notificationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATION_ENABLED] ?: Defaults.NOTIFICATION_ENABLED
    }

    val lastScanTime: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_SCAN_TIME]
    }

    val shakeToFeedback: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHAKE_TO_FEEDBACK] ?: Defaults.SHAKE_TO_FEEDBACK
    }

    val privacyLock: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PRIVACY_LOCK] ?: Defaults.PRIVACY_LOCK
    }

    // ── Suspend Setters ────────────────────────────────────────────────────
    suspend fun setScanConcurrency(value: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SCAN_CONCURRENCY] = value
        }
    }

    suspend fun setHighPrecisionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HIGH_PRECISION_ENABLED] = enabled
        }
    }

    suspend fun setAutoClearRecycle(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_CLEAR_RECYCLE] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = lang
        }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun setLastScanTime(time: Long?) {
        context.dataStore.edit { prefs ->
            if (time != null) {
                prefs[Keys.LAST_SCAN_TIME] = time
            } else {
                prefs.remove(Keys.LAST_SCAN_TIME)
            }
        }
    }

    suspend fun setShakeToFeedback(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHAKE_TO_FEEDBACK] = enabled
        }
    }

    suspend fun setPrivacyLock(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PRIVACY_LOCK] = enabled
        }
    }
}
