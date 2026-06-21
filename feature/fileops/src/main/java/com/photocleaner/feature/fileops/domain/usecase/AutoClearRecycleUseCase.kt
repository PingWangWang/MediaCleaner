package com.photocleaner.feature.fileops.domain.usecase

import com.photocleaner.feature.fileops.domain.FileOperator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自动清理回收站过期项目的定时用例。
 *
 * 通常在应用启动、进入后台一段时间后或定期调度时调用。
 * 内部委托给 [FileOperator.clearExpiredItems] 完成实际的过期删除逻辑。
 *
 * 过期判定依据：[RecycleItemEntity.expireTime] < 当前时间戳。
 * 过期项目会被永久删除（同时删除数据库记录和回收目录中的物理文件）。
 *
 * @property fileOperator 文件操作接口
 *
 * @author PhotoCleaner
 */
@Singleton
class AutoClearRecycleUseCase @Inject constructor(
    private val fileOperator: FileOperator
) {

    /**
     * 执行过期清理。
     *
     * 此方法是挂起函数，应在 [Dispatchers.IO] 或后台协程中调用。
     */
    suspend operator fun invoke() {
        fileOperator.clearExpiredItems()
    }
}
