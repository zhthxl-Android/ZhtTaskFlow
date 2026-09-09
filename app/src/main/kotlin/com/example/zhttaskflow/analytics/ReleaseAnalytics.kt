package com.example.zhttaskflow.analytics

import com.example.zhttaskflow.base.analytics.Analytics
import com.example.zhttaskflow.base.analytics.PageViewEvent
import com.example.zhttaskflow.observability.ReleaseObservabilityContract
import com.example.zhttaskflow.observability.ReleaseObservabilityContract.Channel
import com.example.zhttaskflow.observability.LocalLogStore

/**
 * 生产环境 [Analytics]：页面曝光 / 离开 / 交互点击经 [ReleaseObservabilityContract] 统一字段上报。
 */
object ReleaseAnalytics : Analytics {

    override fun trackPageView(
        pageId: String,
        pageArgs: String?,
        event: PageViewEvent,
    ) {
        if (event == PageViewEvent.Enter || event == PageViewEvent.ArgsChange) {
            LocalLogStore.updateLastKnownPageId(pageId)
        }
        val actionId = when (event) {
            PageViewEvent.Enter -> ReleaseObservabilityContract.ACTION_PAGE_ENTER
            PageViewEvent.ArgsChange -> ReleaseObservabilityContract.ACTION_PAGE_ARGS_CHANGE
        }
        val params = buildMap {
            put("lifecycle", event.name)
            if (!pageArgs.isNullOrBlank()) {
                put("pageArgs", pageArgs)
            }
        }
        ReleaseObservabilityContract.emit(
            channel = Channel.ANALYTICS,
            eventOrMetric = actionId,
            pageId = pageId,
            actionId = actionId,
            params = params,
        )
    }

    override fun trackPageLeave(pageId: String) {
        ReleaseObservabilityContract.emit(
            channel = Channel.ANALYTICS,
            eventOrMetric = ReleaseObservabilityContract.ACTION_PAGE_LEAVE,
            pageId = pageId,
            actionId = ReleaseObservabilityContract.ACTION_PAGE_LEAVE,
            params = mapOf("lifecycle" to "Leave"),
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
        if (!pageId.isNullOrBlank()) {
            LocalLogStore.updateLastKnownPageId(pageId)
        }
        val mergedParams = buildMap {
            put("uiAction", action)
            if (!logTag.isNullOrBlank()) {
                put("logTag", logTag)
            }
            params?.forEach { (key, value) ->
                if (value != null) {
                    put(key, value)
                }
            }
            if (!detail.isNullOrBlank()) {
                put("detail", detail)
            }
        }
        ReleaseObservabilityContract.emit(
            channel = Channel.ANALYTICS,
            eventOrMetric = "ui_interaction",
            pageId = pageId,
            actionId = operationId,
            params = mergedParams,
        )
    }
}
