/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 去重模块 Hilt 依赖注入模块
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.duplicate.di

import com.photocleaner.feature.duplicate.lsh.LshClusterAlgorithm
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DuplicateModule {

    @Provides
    @Singleton
    fun provideLshClusterAlgorithm(): LshClusterAlgorithm = LshClusterAlgorithm
}
