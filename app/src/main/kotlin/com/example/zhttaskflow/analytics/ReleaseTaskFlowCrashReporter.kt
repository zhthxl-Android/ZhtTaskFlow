package com.example.zhttaskflow.analytics

import android.util.Log
import com.example.zhttaskflow.base.exception.TASKFLOW_CRASH_ACTION_ID
import com.example.zhttaskflow.base.exception.TASKFLOW_CRASH_LOG_TAG
import com.example.zhttaskflow.base.exception.TASKFLOW_CRASH_PAGE_ID
import com.example.zhttaskflow.base.exception.TaskFlowCrashReporter
import com.example.zhttaskflow.core.util.nullIfBlank

/**
 * 生产环境 [TaskFlowCrashReporter]：与 [TaskFlowDebugCrashReporter] 字段对齐，对接点见 [dispatchToCrashSdk]。
 */
object ReleaseTaskFlowCrashReporter : TaskFlowCrashReporter {

    override fun reportCrash(throwable: Throwable, fatal: Boolean) {
        val scene = if (fatal) "fatal" else "non_fatal"
        val payload = buildString {
            append("scene=$scene")
            append(" pageId=$TASKFLOW_CRASH_PAGE_ID")
            append(" actionId=$TASKFLOW_CRASH_ACTION_ID")
            append(" type=${throwable::class.simpleName.orEmpty()}")
            val message = throwable.message.nullIfBlank()
            if (!message.isNullOrEmpty()) {
                append(" message=$message")
            }
        }
        dispatchToCrashSdk(
            fatal = fatal,
            throwable = throwable,
            payload = payload,
        )
    }

    /**
     * 预留：对接 Bugly / Firebase Crashlytics 等（`fatal`、`throwable` 与调试版语义一致）。
     */
    private fun dispatchToCrashSdk(
        fatal: Boolean,
        throwable: Throwable,
        payload: String,
    ) {
        // TODO: Bugly.postException(throwable) / Crashlytics.recordException(throwable)
        Log.e(resolveLogTag(), payload, throwable)
    }

    private fun resolveLogTag(): String = "TaskFlow/$TASKFLOW_CRASH_LOG_TAG"
}
