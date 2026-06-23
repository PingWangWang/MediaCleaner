/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 扫描记录模型与持久化存储
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** 独立的 DataStore 文件 */ 
private val Context.scanRecordDataStore by preferencesDataStore(name = "scan_records")
private val RECORDS_KEY = stringPreferencesKey("records")
private const val MAX_RECORDS = 20

/**
 * 扫描记录。
 */
data class ScanRecord(
    val totalImages: Int,
    val duplicateGroups: Int,
    val timestamp: Long,
    val hasDetected: Boolean = false,
    val errorMessage: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("ti", totalImages)
        put("dg", duplicateGroups)
        put("ts", timestamp)
        put("hd", hasDetected)
        put("em", errorMessage ?: "")
    }

    companion object {
        fun fromJson(obj: JSONObject) = ScanRecord(
            totalImages = obj.optInt("ti"),
            duplicateGroups = obj.optInt("dg"),
            timestamp = obj.optLong("ts"),
            hasDetected = obj.optBoolean("hd"),
            errorMessage = if (obj.isNull("em")) null else obj.optString("em", "")
        )

        fun toJsonArray(records: List<ScanRecord>): String =
            JSONArray(records.map { it.toJson() }).toString()

        fun fromJsonArray(json: String): List<ScanRecord> =
            JSONArray(json).let { arr ->
                (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
            }
    }
}

object ScanRecordStore {
    fun getRecords(context: Context): Flow<List<ScanRecord>> =
        context.scanRecordDataStore.data.map { prefs ->
            val json = prefs[RECORDS_KEY]
            if (json.isNullOrEmpty()) emptyList()
            else ScanRecord.fromJsonArray(json).sortedByDescending { it.timestamp }
        }

    suspend fun addRecord(context: Context, record: ScanRecord) {
        context.scanRecordDataStore.edit { prefs ->
            val json = prefs[RECORDS_KEY]
            val list = if (json.isNullOrEmpty()) mutableListOf()
            else ScanRecord.fromJsonArray(json).toMutableList()
            list.add(0, record)
            if (list.size > MAX_RECORDS) list.removeAt(list.size - 1)
            prefs[RECORDS_KEY] = ScanRecord.toJsonArray(list)
        }
    }

    suspend fun updateLastRecord(context: Context, update: (ScanRecord) -> ScanRecord) {
        context.scanRecordDataStore.edit { prefs ->
            val json = prefs[RECORDS_KEY]
            if (json.isNullOrEmpty()) return@edit
            val list = ScanRecord.fromJsonArray(json).toMutableList()
            if (list.isNotEmpty()) {
                list[0] = update(list[0])
                prefs[RECORDS_KEY] = ScanRecord.toJsonArray(list)
            }
        }
    }
}
