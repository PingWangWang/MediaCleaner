/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * Room 类型转换器，支持 List<Long> 存储
 *
 * @author PhotoCleaner
 */
package com.photocleaner.core.database.converter

import androidx.room.TypeConverter

/**
 * Room type converters for the [com.photocleaner.core.database] module.
 *
 * While the current schema uses only primitive and String types, these converters
 * are available for future use (e.g. storing lists, enums, or custom value objects).
 */
class ListTypeConverter {

    /**
     * Converts a comma-separated [String] of Long values back into a [List<Long>].
     * Used when a single column stores multiple numeric ids.
     *
     * @param value Comma-separated Long string, e.g. "1,2,3". Null → empty list.
     * @return Parsed list of Long values.
     */
    @TypeConverter
    fun fromStringToLongList(value: String?): List<Long> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    /**
     * Converts a [List<Long>] into a comma-separated [String] for storage.
     *
     * @param list List of Long values. Null or empty → null.
     * @return Comma-separated string, e.g. "1,2,3".
     */
    @TypeConverter
    fun fromLongListToString(list: List<Long>?): String? {
        if (list.isNullOrEmpty()) return null
        return list.joinToString(",")
    }

    /**
     * Converts a comma-separated [String] of String values back into a [List<String>].
     *
     * @param value Comma-separated string, e.g. "a,b,c". Null → empty list.
     * @return Parsed list of String values.
     */
    @TypeConverter
    fun fromStringToStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").map { it.trim() }
    }

    /**
     * Converts a [List<String>] into a comma-separated [String] for storage.
     *
     * @param list List of String values. Null or empty → null.
     * @return Comma-separated string, e.g. "a,b,c".
     */
    @TypeConverter
    fun fromStringListToString(list: List<String>?): String? {
        if (list.isNullOrEmpty()) return null
        return list.joinToString(",")
    }
}
