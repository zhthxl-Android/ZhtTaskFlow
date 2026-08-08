package com.example.zhttaskflow.base.extension

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 协程通用扩展，比如安全启动协程、异常捕获封装、线程切换简写
 * */


/**
 * 在 [CoroutineScope] 中启动任务，自动忽略 [CancellationException] 的误报日志场景由调用方处理。
 */
fun CoroutineScope.launchCatching(
    onError: (Throwable) -> Unit = {},
    block: suspend CoroutineScope.() -> Unit,
): Job = launch {
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        onError(throwable)
    }
}
