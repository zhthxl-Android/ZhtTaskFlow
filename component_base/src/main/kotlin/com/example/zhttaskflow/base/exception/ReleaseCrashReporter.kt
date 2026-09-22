package com.example.zhttaskflow.base.exception

import com.example.zhttaskflow.base.observability.LocalObservabilityEmitter
import com.example.zhttaskflow.base.observability.ObservabilityEmitPipeline
import com.example.zhttaskflow.base.observability.ReleaseCrashCore

/**
 * 生产环境 [CrashReporter]：未捕获异常与 ANR 经统一可观测管道落盘，并由壳层 [com.example.zhttaskflow.base.observability.ObservabilityPlatformHook] 扩展。
 */
object ReleaseCrashReporter : CrashReporter {

    private val emitAdapter = ReleaseCrashCore.CrashEmit { eventOrMetric, pageId, actionId, params, throwable ->
        ObservabilityEmitPipeline.emit(
            channel = LocalObservabilityEmitter.Channel.CRASH,
            eventOrMetric = eventOrMetric,
            pageId = pageId,
            actionId = actionId,
            params = params,
            throwable = throwable,
        )
    }

    override fun reportCrash(throwable: Throwable, fatal: Boolean) {
        ReleaseCrashCore.reportCrash(
            throwable = throwable,
            fatal = fatal,
            emit = emitAdapter,
        )
    }

    /**
     * ANR 上报（主线程阻塞等）；与 [reportCrash] 共用本地存储与平台扩展位。
     */
    fun reportAnr(threadDump: String) {
        ReleaseCrashCore.reportAnr(
            threadDump = threadDump,
            emit = emitAdapter,
        )
    }
}
