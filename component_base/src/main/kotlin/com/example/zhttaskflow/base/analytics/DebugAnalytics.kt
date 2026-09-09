package com.example.zhttaskflow.base.analytics

import com.example.zhttaskflow.core.log.Logger
import com.example.zhttaskflow.core.util.orEmpty

private const val PAGE_LIFECYCLE_LOG_TAG = "PageLifecycle"
private const val UI_CLICK_LOG_TAG = "UiClick"
private const val UI_LONG_CLICK_LOG_TAG = "UiLongClick"
private const val UI_OUTCOME_LOG_TAG = "UiOutcome"

/**
 * 调试版埋点实现：经 [Logger] 输出，与改造前 PageLifecycle / 交互日志格式完全一致。
 *
 * 产品环境请实现 [Analytics] 并替换 [LocalAnalytics] 注入，勿直接引用本对象。
 */
object DebugAnalytics : Analytics {

    override fun trackPageView(
        pageId: String,
        pageArgs: String?,
        event: PageViewEvent,
    ) {
        Logger.d(PAGE_LIFECYCLE_LOG_TAG) {
            when (event) {
                PageViewEvent.Enter -> {
                    "onEnter page=$pageId args=${pageArgs.orEmpty()}"
                }
                PageViewEvent.ArgsChange -> {
                    "onArgsChange page=$pageId args=${pageArgs.orEmpty()}"
                }
            }
        }
    }

    override fun trackPageLeave(pageId: String) {
        Logger.d(PAGE_LIFECYCLE_LOG_TAG) {
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
        Logger.d(tag) {
            AnalyticsMessageFormatter.formatInteraction(
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
