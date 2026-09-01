package com.example.zhttaskflow.base.analytics

import com.example.zhttaskflow.core.log.TaskFlowLogger
import com.example.zhttaskflow.core.util.orEmpty

private const val PAGE_LIFECYCLE_LOG_TAG = "PageLifecycle"
private const val UI_CLICK_LOG_TAG = "UiClick"
private const val UI_LONG_CLICK_LOG_TAG = "UiLongClick"
private const val UI_OUTCOME_LOG_TAG = "UiOutcome"

/**
 * 调试版埋点实现：经 [TaskFlowLogger] 输出，与改造前 PageLifecycle / 交互日志格式完全一致。
 *
 * 产品环境请实现 [TaskFlowAnalytics] 并替换 [LocalTaskFlowAnalytics] 注入，勿直接引用本对象。
 */
object TaskFlowDebugAnalytics : TaskFlowAnalytics {

    override fun trackPageView(
        pageId: String,
        pageArgs: String?,
        event: TaskFlowPageViewEvent,
    ) {
        TaskFlowLogger.d(PAGE_LIFECYCLE_LOG_TAG) {
            when (event) {
                TaskFlowPageViewEvent.Enter -> {
                    "onEnter page=$pageId args=${pageArgs.orEmpty()}"
                }
                TaskFlowPageViewEvent.ArgsChange -> {
                    "onArgsChange page=$pageId args=${pageArgs.orEmpty()}"
                }
            }
        }
    }

    override fun trackPageLeave(pageId: String) {
        TaskFlowLogger.d(PAGE_LIFECYCLE_LOG_TAG) {
            "onLeave page=$pageId"
        }
    }

    override fun trackInteraction(
        action: String,
        operationId: String,
        pageId: String?,
        params: Map<String, String?>?,
        detail: String?,
        logTag: String?,
    ) {
        val tag = logTag ?: defaultLogTagForAction(action)
        TaskFlowLogger.d(tag) {
            TaskFlowAnalyticsMessageFormatter.formatInteraction(
                action = action,
                operationId = operationId,
                pageId = pageId,
                params = params,
                detail = detail,
            )
        }
    }

    private fun defaultLogTagForAction(action: String): String {
        return when (action) {
            "success", "failure", "info" -> UI_OUTCOME_LOG_TAG
            "longClick" -> UI_LONG_CLICK_LOG_TAG
            else -> UI_CLICK_LOG_TAG
        }
    }
}
