package com.photocleaner.core.common.utils

import java.io.File
import java.io.FileInputStream
import java.security.DigestInputStream
import java.security.MessageDigest

/**
 * 哈希工具类。
 *
 * 提供 MD5 文件校验和 SHA-256 字符串摘要的静态方法。
 *
 * @author PhotoCleaner
 */
object HashUtils {

    private const val STREAM_BUFFER_SIZE = 8192
    private const val HEX_CHARS = "0123456789abcdef"
    private const val MD5_ALGORITHM = "MD5"
    private const val SHA256_ALGORITHM = "SHA-256"

    /**
     * 计算字节数组的 MD5 校验值。
     *
     * @param data 输入字节数组
     * @return 32 位小写 MD5 十六进制字符串
     */
    fun md5(data: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance(MD5_ALGORITHM)
            bytesToHex(digest.digest(data))
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 计算文件的 MD5 校验值。
     *
     * 以 8KB 缓冲分块读取文件，适用于大文件计算。
     * 若文件不存在或读取失败则返回空字符串。
     *
     * @param file 目标文件
     * @return 32 位小写 MD5 十六进制字符串，失败时返回空字符串
     */
    fun md5(file: File): String {
        return try {
            val digest = MessageDigest.getInstance(MD5_ALGORITHM)
            FileInputStream(file).use { fis ->
                DigestInputStream(fis, digest).use { dis ->
                    val buffer = ByteArray(STREAM_BUFFER_SIZE)
                    while (dis.read(buffer) != -1) {
                        // 持续读取以更新摘要
                    }
                }
            }
            bytesToHex(digest.digest())
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 计算字符串的 SHA-256 摘要。
     *
     * @param input 输入字符串
     * @return 64 位小写 SHA-256 十六进制字符串
     */
    fun sha256(input: String): String {
        return try {
            val digest = MessageDigest.getInstance(SHA256_ALGORITHM)
            val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
            bytesToHex(hashBytes)
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 将字节数组转换为小写十六进制字符串。
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = HEX_CHARS[v ushr 4]
            hexChars[i * 2 + 1] = HEX_CHARS[v and 0x0F]
        }
        return String(hexChars)
    }
}
