/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 汉明距离匹配器
 *
 * @author PhotoCleaner
 */
package com.photocleaner.feature.duplicate.matcher

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 汉明距离匹配器。
 *
 * 用于比较两个等长二进制哈希字符串之间的差异位数。
 *
 * @author PhotoCleaner
 */
@Singleton
class HammingDistanceMatcher @Inject constructor() {

    /**
     * 计算两个二进制哈希字符串之间的汉明距离。
     *
     * 汉明距离即对应位置字符不同的位数。
     *
     * @param hash1 第一个二进制哈希字符串
     * @param hash2 第二个二进制哈希字符串
     * @return 差异位数
     * @throws IllegalArgumentException 如果两个字符串长度不同
     */
    fun computeHammingDistance(hash1: String, hash2: String): Int {
        require(hash1.length == hash2.length) {
            "Hash strings must have the same length, but got ${hash1.length} and ${hash2.length}"
        }

        var distance = 0
        for (i in hash1.indices) {
            if (hash1[i] != hash2[i]) {
                distance++
            }
        }
        return distance
    }

    /**
     * 判断两个哈希是否完全一致（精确重复）。
     *
     * @param hash1 第一个二进制哈希字符串
     * @param hash2 第二个二进制哈希字符串
     * @return 如果汉明距离为 0 则返回 true
     */
    fun isExactMatch(hash1: String, hash2: String): Boolean {
        return computeHammingDistance(hash1, hash2) == 0
    }

    /**
     * 判断两个哈希是否具有高相似度。
     *
     * @param hash1    第一个二进制哈希字符串
     * @param hash2    第二个二进制哈希字符串
     * @param threshold 汉明距离阈值，默认 10（即 64 位中最多 10 位不同）
     * @return 如果汉明距离不超过阈值则返回 true
     */
    fun isHighSimilarity(hash1: String, hash2: String, threshold: Int = 10): Boolean {
        return computeHammingDistance(hash1, hash2) <= threshold
    }
}
