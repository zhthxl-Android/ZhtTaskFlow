package com.example.zhttaskflow.core.coroutines

import com.example.zhttaskflow.base.foundation.TaskFlowLogger
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * 全局协程异常处理器，统一捕获协程运行时异常，兜底防止闪退，同时统一日志上报规则。
 * */

/**
 * 统一协程异常拦截，可传入自定义回调。
 */
fun taskFlowCoroutineExceptionHandler(
    tag: String = "Coroutine",
    onError: (Throwable) -> Unit = {},
): CoroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    TaskFlowLogger.e(tag, throwable.message ?: "Coroutine error", throwable)
    onError(throwable)
}
