package com.photocleaner.core.common.model

/**
 * 重复图片分组数据模型。
 *
 * 表示一组经过相似度比对后归类的重复或相似图片集合，包含分组类型、
 * 推荐保留的最佳图片以及可删除图片列表，用于展示清理建议。
 *
 * @property groupId       分组唯一标识
 * @property images        该组包含的所有图片列表
 * @property similarity    组内相似度（0.0 ~ 1.0），1.0 表示完全一致
 * @property groupType     分组类型：[GroupType] 枚举
 * @property bestImage     推荐保留的最佳图片（分辨率最高、质量最好）
 * @property canDeleteImages 建议删除的图片列表（与最佳图片重复/相似）
 * @property size          清理后可释放的总空间（字节）
 * @property createdAt      分组创建时间（毫秒）
 *
 * @author PhotoCleaner
 */
data class DuplicateGroup(
    val groupId: Long,
    val images: List<ImageItem>,
    val similarity: Float,
    val groupType: GroupType,
    val bestImage: ImageItem? = null,
    val canDeleteImages: List<ImageItem>? = null,
    val size: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 重复图片分组类型枚举。
 *
 * @property EXACT_DUPLICATE     精确重复：文件哈希（MD5）完全一致
 * @property HIGH_SIMILARITY     高相似度：dHash/pHash 差异很小（通常 >= 0.95）
 * @property MEDIUM_SIMILARITY   中等相似度：dHash/pHash 有一定差异（通常 >= 0.80）
 */
enum class GroupType {
    EXACT_DUPLICATE,
    HIGH_SIMILARITY,
    MEDIUM_SIMILARITY
}
