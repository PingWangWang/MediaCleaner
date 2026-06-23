/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 应用级依赖注入模块，提供数据库、DataStore、OkHttp 等全局依赖
 *
 * @author PhotoCleaner
 */
package com.photocleaner.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.photocleaner.core.database.AppDatabase
import com.photocleaner.core.database.dao.DuplicateGroupDao
import com.photocleaner.core.database.dao.ImageDao
import com.photocleaner.core.database.dao.RecycleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "photocleaner_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideImageDao(database: AppDatabase): ImageDao = database.imageDao()

    @Provides
    fun provideDuplicateGroupDao(database: AppDatabase): DuplicateGroupDao = database.duplicateGroupDao()

    @Provides
    fun provideRecycleDao(database: AppDatabase): RecycleDao = database.recycleDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}
