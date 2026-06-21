package com.photocleaner.feature.scanner.data.repository

import com.photocleaner.core.common.model.ImageItem
import com.photocleaner.core.database.dao.ImageDao
import com.photocleaner.core.database.entity.ImageItemEntity
import com.photocleaner.core.database.entity.fromImageItem
import com.photocleaner.core.database.entity.toImageItem
import com.photocleaner.feature.scanner.data.datasource.MediaStoreDataSource
import com.photocleaner.feature.scanner.data.datasource.SafDataSource
import com.photocleaner.feature.scanner.domain.repository.ImageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ImageRepository] 的实现。
 *
 * 协调 [MediaStoreDataSource]、[SafDataSource] 和 [ImageDao]，
 * 完成图片扫描、持久化与查询。
 *
 * @author PhotoCleaner
 */
@Singleton
class ImageRepositoryImpl @Inject constructor(
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val safDataSource: SafDataSource,
    private val imageDao: ImageDao
) : ImageRepository {

    override fun scanAllImages(): Flow<ImageItem> {
        return mediaStoreDataSource.queryAllImages()
            .flatMapConcat { chunk ->
                flow {
                    chunk.forEach { imageItem ->
                        emit(imageItem)
                    }
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override fun scanIncremental(since: Long): Flow<ImageItem> {
        return mediaStoreDataSource.queryImagesModifiedSince(since)
            .flatMapConcat { chunk ->
                flow {
                    chunk.forEach { imageItem ->
                        emit(imageItem)
                    }
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun saveImages(images: List<ImageItem>) = withContext(Dispatchers.IO) {
        val entities = images.map { ImageItemEntity.fromImageItem(it) }
        imageDao.insertAll(entities)
    }

    override suspend fun getImageById(id: Long): ImageItem? = withContext(Dispatchers.IO) {
        imageDao.getById(id)?.toImageItem()
    }

    override fun getImagesByBuckets(sizeBucket: Int, ratioBucket: Int): List<ImageItem> {
        // Note: This runs a suspend DAO call; to keep the interface non-suspend
        // we use runBlocking on IO in a coroutine. However, a cleaner approach
        // would be to make the DAO calls synchronous. We'll use a flow-based
        // approach to remain idiomatic.
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            imageDao.getImagesByBuckets(sizeBucket, ratioBucket)
                .map { it.toImageItem() }
        }
    }

    override fun getUncalculatedImages(limit: Int): List<ImageItem> {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            imageDao.getUncalculatedImages(limit)
                .map { it.toImageItem() }
        }
    }

    override suspend fun updateImageHashes(id: Long, dHash: String?, pHash: String?) = withContext(Dispatchers.IO) {
        imageDao.updateHashes(dHash, pHash, listOf(id))
    }

    override suspend fun deleteImages(ids: List<Long>) = withContext(Dispatchers.IO) {
        imageDao.deleteByIds(ids)
    }
}
