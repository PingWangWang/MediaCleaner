/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 扫描模块 Hilt 依赖注入模块
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.scanner.di

import com.photocleaner.feature.scanner.data.repository.ImageRepositoryImpl
import com.photocleaner.feature.scanner.domain.repository.ImageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ScannerModule {

    @Binds
    @Singleton
    abstract fun bindImageRepository(
        impl: ImageRepositoryImpl
    ): ImageRepository
}
