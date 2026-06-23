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
        val MIN_IMAGE_SIZE_KB = intPreferencesKey("min_image_size_kb")
        val IGNORE_SCREENSHOTS = booleanPreferencesKey("ignore_screenshots")
        val IGNORE_GIF = booleanPreferencesKey("ignore_gif")
        val SIMILARITY_THRESHOLD = intPreferencesKey("similarity_threshold")
        val AUTO_RETAIN_BEST = booleanPreferencesKey("auto_retain_best")
        val CONFIRM_BEFORE_DELETE = booleanPreferencesKey("confirm_before_delete")
        val RECYCLE_BIN_ENABLED = booleanPreferencesKey("recycle_bin_enabled")
        val AUTO_CLEAR_RECYCLE = booleanPreferencesKey("auto_clear_recycle")
        val AUTO_CLEAR_RECYCLE_DAYS = intPreferencesKey("auto_clear_recycle_days")
        val AUTO_CHECK_UPDATE = booleanPreferencesKey("auto_check_update")
        val SCAN_ON_WIFI_ONLY = booleanPreferencesKey("scan_on_wifi_only")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val NOTIFICATION_ENABLED = booleanPreferencesKey("notification_enabled")
        val LAST_SCAN_TIME = longPreferencesKey("last_scan_time")
        val SHAKE_TO_FEEDBACK = booleanPreferencesKey("shake_to_feedback")
        val PRIVACY_LOCK = booleanPreferencesKey("privacy_lock")
        val PRIVACY_POLICY_ACCEPTED = booleanPreferencesKey("privacy_policy_accepted")
        val DEVICE_TIER = stringPreferencesKey("device_tier")
    }

    // ── Defaults ───────────────────────────────────────────────────────────
    object Defaults {
        const val SCAN_CONCURRENCY = 4
        const val HIGH_PRECISION_ENABLED = false
        const val MIN_IMAGE_SIZE_KB = 100
        const val IGNORE_SCREENSHOTS = false
        const val IGNORE_GIF = true
        const val SIMILARITY_THRESHOLD = 10
        const val AUTO_RETAIN_BEST = true
        const val CONFIRM_BEFORE_DELETE = true
        const val RECYCLE_BIN_ENABLED = true
        const val AUTO_CLEAR_RECYCLE = false
        const val AUTO_CLEAR_RECYCLE_DAYS = 30
        const val AUTO_CHECK_UPDATE = true
        const val SCAN_ON_WIFI_ONLY = true
        const val THEME_MODE = "SYSTEM"
        const val LANGUAGE = "en"
        const val NOTIFICATION_ENABLED = true
        val LAST_SCAN_TIME: Long? = null
        const val SHAKE_TO_FEEDBACK = true
        const val PRIVACY_LOCK = false
        const val PRIVACY_POLICY_ACCEPTED = false
        const val DEVICE_TIER = "MEDIUM"
    }

    // ── Flow-based Getters ─────────────────────────────────────────────────
    val scanConcurrency: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.SCAN_CONCURRENCY] ?: Defaults.SCAN_CONCURRENCY
    }

    val highPrecisionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HIGH_PRECISION_ENABLED] ?: Defaults.HIGH_PRECISION_ENABLED
    }

    val minImageSizeKb: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.MIN_IMAGE_SIZE_KB] ?: Defaults.MIN_IMAGE_SIZE_KB
    }

    val ignoreScreenshots: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IGNORE_SCREENSHOTS] ?: Defaults.IGNORE_SCREENSHOTS
    }

    val ignoreGif: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IGNORE_GIF] ?: Defaults.IGNORE_GIF
    }

    val similarityThreshold: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.SIMILARITY_THRESHOLD] ?: Defaults.SIMILARITY_THRESHOLD
    }

    val autoRetainBest: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_RETAIN_BEST] ?: Defaults.AUTO_RETAIN_BEST
    }

    val confirmBeforeDelete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.CONFIRM_BEFORE_DELETE] ?: Defaults.CONFIRM_BEFORE_DELETE
    }

    val recycleBinEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.RECYCLE_BIN_ENABLED] ?: Defaults.RECYCLE_BIN_ENABLED
    }

    val autoClearRecycle: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_CLEAR_RECYCLE] ?: Defaults.AUTO_CLEAR_RECYCLE
    }

    val autoClearRecycleDays: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_CLEAR_RECYCLE_DAYS] ?: Defaults.AUTO_CLEAR_RECYCLE_DAYS
    }

    val autoCheckUpdate: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.AUTO_CHECK_UPDATE] ?: Defaults.AUTO_CHECK_UPDATE
    }

    val scanOnWifiOnly: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SCAN_ON_WIFI_ONLY] ?: Defaults.SCAN_ON_WIFI_ONLY
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

    val privacyPolicyAccepted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.PRIVACY_POLICY_ACCEPTED] ?: Defaults.PRIVACY_POLICY_ACCEPTED
    }

    val deviceTier: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.DEVICE_TIER] ?: Defaults.DEVICE_TIER
    }

    // ── Suspend Setters ────────────────────────────────────────────────────
    suspend fun setScanConcurrency(value: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.SCAN_CONCURRENCY] = value }
    }

    suspend fun setHighPrecisionEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.HIGH_PRECISION_ENABLED] = enabled }
    }

    suspend fun setMinImageSizeKb(value: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.MIN_IMAGE_SIZE_KB] = value }
    }

    suspend fun setIgnoreScreenshots(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.IGNORE_SCREENSHOTS] = enabled }
    }

    suspend fun setIgnoreGif(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.IGNORE_GIF] = enabled }
    }

    suspend fun setSimilarityThreshold(value: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.SIMILARITY_THRESHOLD] = value }
    }

    suspend fun setAutoRetainBest(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.AUTO_RETAIN_BEST] = enabled }
    }

    suspend fun setConfirmBeforeDelete(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.CONFIRM_BEFORE_DELETE] = enabled }
    }

    suspend fun setRecycleBinEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.RECYCLE_BIN_ENABLED] = enabled }
    }

    suspend fun setAutoClearRecycle(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.AUTO_CLEAR_RECYCLE] = enabled }
    }

    suspend fun setAutoClearRecycleDays(days: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.AUTO_CLEAR_RECYCLE_DAYS] = days }
    }

    suspend fun setAutoCheckUpdate(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.AUTO_CHECK_UPDATE] = enabled }
    }

    suspend fun setScanOnWifiOnly(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.SCAN_ON_WIFI_ONLY] = enabled }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { prefs -> prefs[Keys.LANGUAGE] = lang }
    }

    suspend fun setNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.NOTIFICATION_ENABLED] = enabled }
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
        context.dataStore.edit { prefs -> prefs[Keys.SHAKE_TO_FEEDBACK] = enabled }
    }

    suspend fun setPrivacyLock(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.PRIVACY_LOCK] = enabled }
    }

    suspend fun setPrivacyPolicyAccepted(accepted: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.PRIVACY_POLICY_ACCEPTED] = accepted }
    }

    suspend fun setDeviceTier(tier: String) {
        context.dataStore.edit { prefs -> prefs[Keys.DEVICE_TIER] = tier }
    }
}
