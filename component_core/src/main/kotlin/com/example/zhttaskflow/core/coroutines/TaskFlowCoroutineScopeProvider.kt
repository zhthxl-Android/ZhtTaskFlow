package com.example.zhttaskflow.core.coroutines

import com.example.zhttaskflow.base.foundation.TaskFlowLogger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 全局协程作用域工厂：由 Application 或壳工程手动创建并持有，不使用 DI。
 *
 * 统一管理全局 / 业务级协程作用域，规范协程生命周期与线程调度。
 */
class TaskFlowCoroutineScopeProvider(
    private val tag: String = "AppScope",
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        TaskFlowLogger.e(tag, "Coroutine uncaught", throwable)
    }

    val applicationScope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + exceptionHandler,
    )

    val ioScope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + exceptionHandler,
    )
}
