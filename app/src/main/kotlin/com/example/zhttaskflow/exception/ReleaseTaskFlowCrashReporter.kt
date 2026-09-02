package com.example.zhttaskflow.exception

import com.example.zhttaskflow.base.exception.TASKFLOW_CRASH_ACTION_ID
import com.example.zhttaskflow.base.exception.TASKFLOW_CRASH_PAGE_ID
import com.example.zhttaskflow.base.exception.TaskFlowCrashReporter
import com.example.zhttaskflow.core.util.nullIfBlank
import com.example.zhttaskflow.observability.ReleaseTaskFlowObservabilityContract
import com.example.zhttaskflow.observability.ReleaseTaskFlowObservabilityContract.Channel

private const val ANR_MESSAGE_MAX_LENGTH: Int = 2_048

/**
 * 生产环境 [TaskFlowCrashReporter]：未捕获异常与 ANR 经 [ReleaseTaskFlowObservabilityContract] 对接崩溃平台。
 *
 * ANR：在 Application 中调用 [ReleaseTaskFlowCrashMonitoring.install]（或接入 Bugly 等 SDK 自带 ANR）后，
 * 由监控层回调 [reportAnr]。
 */
object ReleaseTaskFlowCrashReporter : TaskFlowCrashReporter {

    override fun reportCrash(throwable: Throwable, fatal: Boolean) {
        val scene = if (fatal) "fatal" else "non_fatal"
        ReleaseTaskFlowObservabilityContract.emit(
            channel = Channel.CRASH,
            eventOrMetric = ReleaseTaskFlowObservabilityContract.EVENT_CRASH,
            pageId = TASKFLOW_CRASH_PAGE_ID,
            actionId = TASKFLOW_CRASH_ACTION_ID,
            params = mapOf(
                "scene" to scene,
                "type" to throwable::class.simpleName.orEmpty(),
                "message" to throwable.message.nullIfBlank().orEmpty(),
            ),
            throwable = throwable,
        )
    }

    /**
     * ANR 上报（主线程阻塞等）；与 [reportCrash] 共用崩溃平台契约。
     */
    fun reportAnr(threadDump: String) {
        val synthetic = AnrReportException(threadDump.take(ANR_MESSAGE_MAX_LENGTH))
        ReleaseTaskFlowObservabilityContract.emit(
            channel = Channel.CRASH,
            eventOrMetric = ReleaseTaskFlowObservabilityContract.EVENT_ANR,
            pageId = TASKFLOW_CRASH_PAGE_ID,
            actionId = ReleaseTaskFlowObservabilityContract.ACTION_APP_ANR,
            params = mapOf(
                "scene" to "anr",
                "dumpLength" to threadDump.length.toString(),
            ),
            throwable = synthetic,
        )
    }

    private class AnrReportException(message: String) : RuntimeException(message)
}
