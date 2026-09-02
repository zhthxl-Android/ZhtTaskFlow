package com.example.zhttaskflow.analytics

import com.example.zhttaskflow.base.analytics.TaskFlowAnalytics
import com.example.zhttaskflow.base.analytics.TaskFlowPageViewEvent
import com.example.zhttaskflow.observability.ReleaseTaskFlowObservabilityContract
import com.example.zhttaskflow.observability.ReleaseTaskFlowObservabilityContract.Channel
import com.example.zhttaskflow.observability.TaskFlowLocalLogStore

/**
 * 生产环境 [TaskFlowAnalytics]：页面曝光 / 离开 / 交互点击经 [ReleaseTaskFlowObservabilityContract] 统一字段上报。
 */
object ReleaseTaskFlowAnalytics : TaskFlowAnalytics {

    override fun trackPageView(
        pageId: String,
        pageArgs: String?,
        event: TaskFlowPageViewEvent,
    ) {
        if (event == TaskFlowPageViewEvent.Enter || event == TaskFlowPageViewEvent.ArgsChange) {
            TaskFlowLocalLogStore.updateLastKnownPageId(pageId)
        }
        val actionId = when (event) {
            TaskFlowPageViewEvent.Enter -> ReleaseTaskFlowObservabilityContract.ACTION_PAGE_ENTER
            TaskFlowPageViewEvent.ArgsChange -> ReleaseTaskFlowObservabilityContract.ACTION_PAGE_ARGS_CHANGE
        }
        val params = buildMap {
            put("lifecycle", event.name)
            if (!pageArgs.isNullOrBlank()) {
                put("pageArgs", pageArgs)
            }
        }
        ReleaseTaskFlowObservabilityContract.emit(
            channel = Channel.ANALYTICS,
            eventOrMetric = actionId,
            pageId = pageId,
            actionId = actionId,
            params = params,
        )
    }

    override fun trackPageLeave(pageId: String) {
        ReleaseTaskFlowObservabilityContract.emit(
            channel = Channel.ANALYTICS,
            eventOrMetric = ReleaseTaskFlowObservabilityContract.ACTION_PAGE_LEAVE,
            pageId = pageId,
            actionId = ReleaseTaskFlowObservabilityContract.ACTION_PAGE_LEAVE,
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
            TaskFlowLocalLogStore.updateLastKnownPageId(pageId)
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
        ReleaseTaskFlowObservabilityContract.emit(
            channel = Channel.ANALYTICS,
            eventOrMetric = "ui_interaction",
            pageId = pageId,
            actionId = operationId,
            params = mergedParams,
        )
    }
}
