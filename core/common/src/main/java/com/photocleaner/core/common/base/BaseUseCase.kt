/*
 * Copyright (C) 2025 PhotoCleaner
 *
 * 用例基类，定义 UseCase 执行模板
 *
 * @author PhotoCleaner
 */
package com.photocleaner.core.common.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 用例（UseCase）抽象基类。
 *
 * 封装单一业务操作，通过 [invoke] 运算符以挂起方式执行并返回 [Result]<[T]>。
 * 子类需实现 [execute] 方法提供具体逻辑，并可选择覆写 [coroutineDispatcher]
 * 以指定执行所用的调度器（默认为 [Dispatchers.IO]）。
 *
 * 使用示例：
 * ```
 * class GetUserUseCase @Inject constructor(
 *     private val userRepository: UserRepository
 * ) : BaseUseCase<User>() {
 *     override suspend fun execute(): Result<User> {
 *         return userRepository.getUser()
 *     }
 * }
 * ```
 *
 * @param T 用例返回的数据类型
 * @author PhotoCleaner
 */
abstract class BaseUseCase<T> {

    /**
     * 协程调度器，子类可覆写以更改执行线程。
     * 默认为 [Dispatchers.IO]，适合 I/O 密集型操作。
     */
    protected open val coroutineDispatcher: CoroutineDispatcher
        get() = Dispatchers.IO

    /**
     * 受保护的 [CoroutineDispatcher] 的别名引用，供子类直接使用。
     */
    protected val dispatcher: CoroutineDispatcher
        get() = coroutineDispatcher

    /**
     * 运算符重载：以挂起方式执行用例。
     *
     * @return [Result] 封装执行成功的结果或失败异常
     */
    suspend operator fun invoke(): Result<T> {
        return try {
            Result.success(execute())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 子类需实现的业务逻辑。
     *
     * @return 用例执行结果
     */
    protected abstract suspend fun execute(): T
}
