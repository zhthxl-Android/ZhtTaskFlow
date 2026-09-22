package com.example.zhttaskflow.observability

import com.example.zhttaskflow.base.observability.LocalObservabilityEmitter
import com.example.zhttaskflow.base.observability.ObservabilityEmitPipeline

/**
 * app 模块可观测 emit 入口：委托 base [ObservabilityEmitPipeline]（性能 / 崩溃等 Release 实现沿用）。
 */
internal object ReleaseObservabilityContract {

    fun emit(
        channel: LocalObservabilityEmitter.Channel,
        eventOrMetric: String,
        pageId: String?,
        actionId: String?,
        params: Map<String, String?>? = null,
        throwable: Throwable? = null,
    ) {
        ObservabilityEmitPipeline.emit(
            channel = channel,
            eventOrMetric = eventOrMetric,
            pageId = pageId,
            actionId = actionId,
            params = params,
            throwable = throwable,
        )
    }

    fun formatParams(params: Map<String, String?>?): String {
        return ObservabilityEmitPipeline.formatParams(params)
    }
}
