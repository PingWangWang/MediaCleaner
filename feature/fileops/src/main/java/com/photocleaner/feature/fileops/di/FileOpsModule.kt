/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 文件操作模块 Hilt 依赖注入模块
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.fileops.di

import com.photocleaner.feature.fileops.data.FileOperatorImpl
import com.photocleaner.feature.fileops.domain.FileOperator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FileOpsModule {

    @Binds
    @Singleton
    abstract fun bindFileOperator(
        impl: FileOperatorImpl
    ): FileOperator
}
