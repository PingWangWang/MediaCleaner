/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * DetectDuplicateUseCase 单元测试
 *
 * 由于 DetectDuplicateUseCase 依赖多个复杂组件（DHashCalculator, PHashCalculator,
 * HammingDistanceMatcher, LshClusterAlgorithm, SimilarityGraph, OrbFeatureMatcher），
 * 所有依赖均为具体的 Kotlin 类或 object，可直接实例化。
 *
 * 测试策略：
 * - 通过提供预计算的 dHash/pHash 值来绕过 loadBitmap（path = null 使 loadBitmap 返回 null）
 * - 利用 LshClusterAlgorithm 的真实行为：相同哈希 → 全部碰撞 → 同一聚类；
 *   完全不同哈希 → 无碰撞 → 无聚类
 * - 无需 mock 框架，纯真实对象 + 可控输入数据隔离测试
 */
package com.photocleaner.feature.duplicate.domain.usecase

import com.photocleaner.core.common.model.GroupType
import com.photocleaner.core.common.model.ImageItem
import com.photocleaner.feature.duplicate.graph.SimilarityGraph
import com.photocleaner.feature.duplicate.hash.DHashCalculator
import com.photocleaner.feature.duplicate.hash.PHashCalculator
import com.photocleaner.feature.duplicate.lsh.LshClusterAlgorithm
import com.photocleaner.feature.duplicate.matcher.HammingDistanceMatcher
import com.photocleaner.feature.duplicate.matcher.OrbFeatureMatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * DetectDuplicateUseCase 的单元测试。
 *
 * 测试涵盖空列表、单张图片、相同哈希分组、不同哈希隔离等基础场景。
 */
class DetectDuplicateUseCaseTest {

    private lateinit var useCase: DetectDuplicateUseCase

    @Before
    fun setup() {
        useCase = DetectDuplicateUseCase(
            dHashCalculator = DHashCalculator(),
            pHashCalculator = PHashCalculator(),
            hammingDistanceMatcher = HammingDistanceMatcher(),
            lshClusterAlgorithm = LshClusterAlgorithm,
            similarityGraph = SimilarityGraph(),
            orbFeatureMatcher = OrbFeatureMatcher()
        )
    }

    /**
     * 创建一个测试用 ImageItem。
     *
     * @param id          图片 ID
     * @param dHash       dHash 值（64 位二进制字符串）
     * @param pHash       pHash 值（可选）
     * @param sizeBucket  大小分桶（默认 0，所有图片在同一桶）
     * @param ratioBucket 宽高比分桶（默认 0，所有图片在同一桶）
     */
    private fun createImage(
        id: Long,
        dHash: String?,
        pHash: String? = null,
        sizeBucket: Int = 0,
        ratioBucket: Int = 0
    ): ImageItem {
        return ImageItem(
            id = id,
            path = null,             // path = null 使 loadBitmap 返回 null，避免 Android 依赖
            uri = "content://test/$id",
            name = "img_$id.jpg",
            size = 1024,
            modifyTime = System.currentTimeMillis(),
            dHash = dHash,
            pHash = pHash,
            sizeBucket = sizeBucket,
            ratioBucket = ratioBucket
        )
    }

    /**
     * 收集 Flow 中的所有元素并返回。
     */
    private suspend fun <T> Flow<T>.collectAll(): List<T> = toList()

    // ──────────────────────────────────────────────
    // 测试用例
    // ──────────────────────────────────────────────

    /**
     * 场景 1：空图片列表应返回空流。
     *
     * 输入：空列表
     * 预期：无任何 DuplicateGroup 发射
     */
    @Test
    fun `empty image list returns empty flow`() = runBlocking {
        val results = useCase(emptyList()).collectAll()
        assertEquals(0, results.size)
    }

    /**
     * 场景 2：单张图片应返回空流（无需检测重复）。
     *
     * 输入：1 张图片
     * 预期：无任何 DuplicateGroup 发射
     */
    @Test
    fun `single image returns empty flow`() = runBlocking {
        val hash = "1010101010101010101010101010101010101010101010101010101010101010"
        val images = listOf(createImage(1, hash))
        val results = useCase(images).collectAll()
        assertEquals(0, results.size)
    }

    /**
     * 场景 3：完全相同 dHash 的图片应分到同一组。
     *
     * 3 张图片使用完全相同的 64 位 dHash，预期：
     * - 产生 1 个 DuplicateGroup
     * - 组内包含 3 张图片
     * - GroupType 为 EXACT_DUPLICATE（汉明距离 = 0）
     * - 相似度为 1.0
     */
    @Test
    fun `images with identical dHash are grouped together`() = runBlocking {
        val hash = "1010101010101010101010101010101010101010101010101010101010101010"
        val images = listOf(
            createImage(1, hash),
            createImage(2, hash),
            createImage(3, hash)
        )
        val results = useCase(images).collectAll()

        assertEquals("相同哈希应产生 1 个分组", 1, results.size)

        val group = results[0]
        assertEquals("组内应有 3 张图片", 3, group.images.size)
        assertEquals("分组类型应为精确重复", GroupType.EXACT_DUPLICATE, group.groupType)
        assertEquals("相似度应为 1.0", 1.0f, group.similarity, 0.001f)

        // 验证分组中包含所有 3 张图片的 ID
        val idsInGroup = group.images.map { it.id }.toSet()
        assertEquals(setOf(1L, 2L, 3L), idsInGroup)
    }

    /**
     * 场景 4：完全不同 dHash 的图片应分到不同组。
     *
     * 2 张图片使用完全不同的 64 位 dHash（全 0 和全 1），
     * LSH 在任何 band 中都不会碰撞，预期：
     * - 无任何 DuplicateGroup 发射
     */
    @Test
    fun `images with completely different dHash are not grouped`() = runBlocking {
        val hash1 = "0000000000000000000000000000000000000000000000000000000000000000"
        val hash2 = "1111111111111111111111111111111111111111111111111111111111111111"
        val images = listOf(
            createImage(1, hash1),
            createImage(2, hash2)
        )
        val results = useCase(images).collectAll()
        assertEquals("不同哈希的图片不应产生分组", 0, results.size)
    }

    /**
     * 场景 5：多个不同 sizeBucket 的图片应独立分组。
     *
     * 3 张图片在同一 sizeBucket+ratioBucket，3 张在另一 sizeBucket+ratioBucket，
     * 各桶内哈希相同，预期产生 2 个独立分组。
     */
    @Test
    fun `images with different sizeBuckets are grouped separately`() = runBlocking {
        val hash = "1010101010101010101010101010101010101010101010101010101010101010"
        val images = listOf(
            // 桶 1：sizeBucket=0, ratioBucket=0
            createImage(1, hash, sizeBucket = 0, ratioBucket = 0),
            createImage(2, hash, sizeBucket = 0, ratioBucket = 0),
            createImage(3, hash, sizeBucket = 0, ratioBucket = 0),
            // 桶 2：sizeBucket=1, ratioBucket=1
            createImage(4, hash, sizeBucket = 1, ratioBucket = 1),
            createImage(5, hash, sizeBucket = 1, ratioBucket = 1),
            createImage(6, hash, sizeBucket = 1, ratioBucket = 1)
        )
        val results = useCase(images).collectAll()

        assertEquals("两个不同的桶应产生 2 个分组", 2, results.size)

        // 每个分组应包含 3 张图片
        results.forEach { group ->
            assertEquals("每组应有 3 张图片", 3, group.images.size)
        }

        // 验证两个分组没有重叠的图片 ID
        val allIds = results.flatMap { group -> group.images.map { it.id } }.toSet()
        assertEquals("总共 6 张不同的图片", 6, allIds.size)
    }

    /**
     * 场景 6：单张图片在桶中（桶大小 < 2）不应发射分组。
     *
     * 3 张图片分在 3 个不同的 sizeBucket 中，每个桶仅有 1 张图片，
     * 预期无任何分组发射。
     */
    @Test
    fun `single image in each bucket does not emit any group`() = runBlocking {
        val hash = "1010101010101010101010101010101010101010101010101010101010101010"
        val images = listOf(
            createImage(1, hash, sizeBucket = 0, ratioBucket = 0),
            createImage(2, hash, sizeBucket = 1, ratioBucket = 1),
            createImage(3, hash, sizeBucket = 2, ratioBucket = 2)
        )
        val results = useCase(images).collectAll()
        assertEquals("每个桶只有 1 张图片，不应产生分组", 0, results.size)
    }

    /**
     * 场景 7：缺少 dHash 的图片应被过滤掉。
     *
     * 3 张图片中 1 张缺少 dHash（null），1 张 dHash 长度不为 64，
     * 1 张 dHash 有效但只有它自己（桶中仅 1 张有效），预期无分组发射。
     */
    @Test
    fun `images without valid dHash are filtered out`() = runBlocking {
        val validHash = "1010101010101010101010101010101010101010101010101010101010101010"
        val images = listOf(
            createImage(1, dHash = null),                              // dHash 为 null
            createImage(2, dHash = "10101010"),                        // dHash 长度不为 64
            createImage(3, dHash = validHash)                          // 有效但只有自己
        )
        val results = useCase(images).collectAll()
        assertEquals("有效图片不足 2 张，不应产生分组", 0, results.size)
    }
}
