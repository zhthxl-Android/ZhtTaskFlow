package com.example.zhttaskflow.core.network

import com.example.zhttaskflow.core.log.Logger

/**
 * 数据层网络失败诊断：Release 不打印；Debug 仅输出一条 Debug 级详细日志（含堆栈）。
 */
internal fun logSafeApiCallFailure(
    tag: String,
    summary: String,
    throwable: Throwable,
) {
    Logger.d(tag, throwable) { summary }
}
