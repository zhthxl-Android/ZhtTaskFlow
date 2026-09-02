package com.example.zhttaskflow.analytics

import android.util.Log
import com.example.zhttaskflow.base.analytics.TaskFlowAnalytics
import com.example.zhttaskflow.base.analytics.TaskFlowPageViewEvent
import com.example.zhttaskflow.core.util.orEmpty

private const val PAGE_LIFECYCLE_LOG_TAG = "PageLifecycle"

/**
 * 生产环境 [TaskFlowAnalytics] 实现：字段与 [com.example.zhttaskflow.base.analytics.TaskFlowDebugAnalytics] /
 * [com.example.zhttaskflow.base.analytics.TaskFlowAnalyticsMessageFormatter] 对齐（`pageId`、`actionId`、`params`）。
 *
 * 公司埋点 SDK 接入点见 [dispatchToProductSdk]；当前无 SDK 时以标准单行日志落盘，便于 Release 联调与回归。
 */
object ReleaseTaskFlowAnalytics : TaskFlowAnalytics {

    override fun trackPageView(
        pageId: String,
        pageArgs: String?,
        event: TaskFlowPageViewEvent,
    ) {
        val line = when (event) {
            TaskFlowPageViewEvent.Enter -> "onEnter page=$pageId args=${pageArgs.orEmpty()}"
            TaskFlowPageViewEvent.ArgsChange -> "onArgsChange page=$pageId args=${pageArgs.orEmpty()}"
        }
        dispatchToProductSdk(
            category = PAGE_LIFECYCLE_LOG_TAG,
            payload = line,
            pageId = pageId,
            actionId = null,
            params = pageArgs?.let { mapOf("pageArgs" to it) },
        )
    }

    override fun trackPageLeave(pageId: String) {
        val line = "onLeave page=$pageId"
        dispatchToProductSdk(
            category = PAGE_LIFECYCLE_LOG_TAG,
            payload = line,
            pageId = pageId,
            actionId = null,
            params = null,
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
        val line = formatInteraction(
            action = action,
            operationId = operationId,
            pageId = pageId,
            params = params,
            detail = detail,
        )
        dispatchToProductSdk(
            category = logTag ?: categoryForAction(action),
            payload = line,
            pageId = pageId,
            actionId = operationId,
            params = params,
        )
    }

    /**
     * 预留：对接友盟 / 自研等 SDK（`pageId`、`actionId`、`params` 与调试版一致）。
     */
    private fun dispatchToProductSdk(
        category: String,
        payload: String,
        pageId: String?,
        actionId: String?,
        params: Map<String, String?>?,
    ) {
        // TODO: MyCompanyAnalytics.trackEvent(category, pageId, actionId, params)
        Log.i(resolveLogTag(category), payload)
    }

    private fun resolveLogTag(category: String): String = "TaskFlow/$category"

    private fun categoryForAction(action: String): String {
        return when (action) {
            "success", "failure", "info" -> "UiOutcome"
            "longClick" -> "UiLongClick"
            else -> "UiClick"
        }
    }

    private fun formatInteraction(
        action: String,
        operationId: String,
        pageId: String?,
        params: Map<String, String?>?,
        detail: String?,
    ): String {
        val parts = buildList {
            add("action=$action")
            if (!pageId.isNullOrBlank()) {
                add("pageId=$pageId")
            }
            add("actionId=$operationId")
            val snapshot = formatParamsSnapshot(params = params, detail = detail)
            if (snapshot.isNotBlank()) {
                add("params=$snapshot")
            }
        }
        return parts.joinToString(separator = " ")
    }

    private fun formatParamsSnapshot(
        params: Map<String, String?>?,
        detail: String?,
    ): String {
        val fromMap = params
            ?.entries
            ?.mapNotNull { (key, value) ->
                value?.let { safeValue -> "$key=$safeValue" }
            }
            ?.joinToString(separator = ",")
        return when {
            !fromMap.isNullOrBlank() && !detail.isNullOrBlank() -> "$fromMap,$detail"
            !fromMap.isNullOrBlank() -> fromMap
            !detail.isNullOrBlank() -> detail
            else -> ""
        }
    }
}
