/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * Application 类，Hilt 注入入口
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app.di

import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PhotoCleanerApplication : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
