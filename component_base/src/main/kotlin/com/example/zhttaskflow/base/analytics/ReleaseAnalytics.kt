package com.example.zhttaskflow.base.analytics

import com.example.zhttaskflow.base.observability.LocalObservabilityEmitter
import com.example.zhttaskflow.base.observability.ObservabilityEmitPipeline
import com.example.zhttaskflow.base.observability.ReleaseAnalyticsCore

/**
 * 生产环境 [Analytics]：页面曝光 / 离开 / 交互经统一可观测管道落盘，并由壳层 [ObservabilityPlatformHook] 扩展。
 */
object ReleaseAnalytics : Analytics {

    private val emitAdapter = ReleaseAnalyticsCore.AnalyticsEmit { eventOrMetric, pageId, actionId, params ->
        ObservabilityEmitPipeline.emit(
            channel = LocalObservabilityEmitter.Channel.ANALYTICS,
            eventOrMetric = eventOrMetric,
            pageId = pageId,
            actionId = actionId,
            params = params,
        )
    }

    override fun trackPageView(
        pageId: String,
        pageArgs: String?,
        event: PageViewEvent,
    ) {
        ReleaseAnalyticsCore.trackPageView(
            pageId = pageId,
            pageArgs = pageArgs,
            event = event,
            emit = emitAdapter,
        )
    }

    override fun trackPageLeave(pageId: String) {
        ReleaseAnalyticsCore.trackPageLeave(
            pageId = pageId,
            emit = emitAdapter,
        )
    }

    override fun trackInteraction(
        action: String,
        operationId: String,
        pageId: String?,
        params: Map<String, String?>?,
        detail: String?,
        logTag: String?,
    ) {
        ReleaseAnalyticsCore.trackInteraction(
            action = action,
            operationId = operationId,
            pageId = pageId,
            params = params,
            detail = detail,
            logTag = logTag,
            emit = emitAdapter,
        )
    }
}
