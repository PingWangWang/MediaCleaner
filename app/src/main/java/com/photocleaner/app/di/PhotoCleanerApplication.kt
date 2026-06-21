package com.photocleaner.app.di

import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PhotoCleanerApplication : android.app.Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
