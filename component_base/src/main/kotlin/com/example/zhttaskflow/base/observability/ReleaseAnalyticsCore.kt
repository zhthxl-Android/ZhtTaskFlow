package com.example.zhttaskflow.base.observability

import com.example.zhttaskflow.base.analytics.PageViewEvent
import com.example.zhttaskflow.core.observability.LocalLogStore

/**
 * Release 风格埋点共用逻辑：页面 ID 更新、契约字段映射与参数拼装。
 */
internal object ReleaseAnalyticsCore {

    internal fun interface AnalyticsEmit {
        fun emit(
            eventOrMetric: String,
            pageId: String?,
            actionId: String?,
            params: Map<String, String?>?,
        )
    }

    fun trackPageView(
        pageId: String,
        pageArgs: String?,
        event: PageViewEvent,
        emit: AnalyticsEmit,
    ) {
        if (event == PageViewEvent.Enter || event == PageViewEvent.ArgsChange) {
            LocalLogStore.updateLastKnownPageId(pageId)
        }
        val actionId = when (event) {
            PageViewEvent.Enter -> LocalObservabilityEmitter.ACTION_PAGE_ENTER
            PageViewEvent.ArgsChange -> LocalObservabilityEmitter.ACTION_PAGE_ARGS_CHANGE
        }
        val params = buildMap {
            put("lifecycle", event.name)
            if (!pageArgs.isNullOrBlank()) {
                put("pageArgs", pageArgs)
            }
        }
        emit.emit(
            eventOrMetric = actionId,
            pageId = pageId,
            actionId = actionId,
            params = params,
        )
    }

    fun trackPageLeave(
        pageId: String,
        emit: AnalyticsEmit,
    ) {
        emit.emit(
            eventOrMetric = LocalObservabilityEmitter.ACTION_PAGE_LEAVE,
            pageId = pageId,
            actionId = LocalObservabilityEmitter.ACTION_PAGE_LEAVE,
            params = mapOf("lifecycle" to "Leave"),
        )
    }

    fun trackInteraction(
        action: String,
        operationId: String,
        pageId: String?,
        params: Map<String, String?>?,
        detail: String?,
        logTag: String?,
        emit: AnalyticsEmit,
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
        emit.emit(
            eventOrMetric = "ui_interaction",
            pageId = pageId,
            actionId = operationId,
            params = mergedParams,
        )
    }
}
