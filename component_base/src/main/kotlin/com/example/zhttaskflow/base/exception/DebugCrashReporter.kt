package com.example.zhttaskflow.base.exception

import com.example.zhttaskflow.base.analytics.AnalyticsRegistry
import com.example.zhttaskflow.base.analytics.trackUiOutcome
import com.example.zhttaskflow.core.log.Logger
import com.example.zhttaskflow.core.util.nullIfBlank

/**
 * 调试默认上报：[Logger.errorAlways] + Analytics outcome（`pageId=[TASKFLOW_CRASH_PAGE_ID]`、`actionId=[TASKFLOW_CRASH_ACTION_ID]`）。
 */
object DebugCrashReporter : CrashReporter {

    override fun reportCrash(throwable: Throwable, fatal: Boolean) {
        val scene = if (fatal) "fatal" else "non_fatal"
        Logger.errorAlways(TASKFLOW_CRASH_LOG_TAG, {
            "[$scene] ${throwable.message.nullIfBlank() ?: throwable::class.simpleName.orEmpty()}"
        }, throwable)
        AnalyticsRegistry.current().trackUiOutcome(
            outcome = "failure",
            operationId = TASKFLOW_CRASH_ACTION_ID,
            pageId = TASKFLOW_CRASH_PAGE_ID,
            params = mapOf(
                "fatal" to fatal.toString(),
                "type" to throwable::class.simpleName.orEmpty(),
            ),
        )
    }
}
