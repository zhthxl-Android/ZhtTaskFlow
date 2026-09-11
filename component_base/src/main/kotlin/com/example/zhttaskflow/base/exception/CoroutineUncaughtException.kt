package com.example.zhttaskflow.base.exception

import com.example.zhttaskflow.core.util.nullIfBlank

/**
 * 协程未捕获异常包装：堆栈保留原始 cause，消息携带页面与协程上下文摘要。
 */
internal class CoroutineUncaughtException(
    pagePath: String,
    coroutineContextSummary: String,
    cause: Throwable,
) : RuntimeException(
    buildString {
        append(COROUTINE_UNCAUGHT_SOURCE)
        append(" pagePath=").append(pagePath)
        append(" context=").append(coroutineContextSummary)
        append(" message=").append(cause.message.nullIfBlank().orEmpty())
    },
    cause,
)
