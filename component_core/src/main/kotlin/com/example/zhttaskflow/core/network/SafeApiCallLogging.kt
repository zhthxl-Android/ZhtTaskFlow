package com.example.zhttaskflow.core.network

import android.util.Log
import com.example.zhttaskflow.base.foundation.TaskFlowLogger

/**
 * 数据层网络失败诊断：Release 不打印 Error；Debug 仅输出一条 Debug 级详细日志（含堆栈）。
 */
internal fun logSafeApiCallFailure(
    tag: String,
    summary: String,
    throwable: Throwable,
) {
    if (!TaskFlowNetworkDiagnostics.isDebuggable) {
        return
    }
    TaskFlowLogger.d(
        tag,
        "$summary\n${Log.getStackTraceString(throwable)}",
    )
}
