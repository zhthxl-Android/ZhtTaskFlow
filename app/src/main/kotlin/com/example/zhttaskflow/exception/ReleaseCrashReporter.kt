package com.example.zhttaskflow.exception

import com.example.zhttaskflow.base.exception.TASKFLOW_CRASH_ACTION_ID
import com.example.zhttaskflow.base.exception.TASKFLOW_CRASH_PAGE_ID
import com.example.zhttaskflow.base.exception.CrashReporter
import com.example.zhttaskflow.core.util.nullIfBlank
import com.example.zhttaskflow.observability.ReleaseObservabilityContract
import com.example.zhttaskflow.observability.ReleaseObservabilityContract.Channel
import com.example.zhttaskflow.observability.LocalLogStore

private const val ANR_MESSAGE_MAX_LENGTH: Int = 2_048

/**
 * 生产环境 [CrashReporter]：未捕获异常与 ANR 写入本地日志仓（含堆栈、设备信息、最近页面路径）。
 *
 * 下次启动可通过 [LocalLogStore.peekLastCrash] 或调试页查看 [last_crash.jsonl]。
 */
object ReleaseCrashReporter : CrashReporter {

    override fun reportCrash(throwable: Throwable, fatal: Boolean) {
        val scene = if (fatal) "fatal" else "non_fatal"
        val pagePath = LocalLogStore.lastKnownPageId().orEmpty()
        ReleaseObservabilityContract.emit(
            channel = Channel.CRASH,
            eventOrMetric = ReleaseObservabilityContract.EVENT_CRASH,
            pageId = TASKFLOW_CRASH_PAGE_ID,
            actionId = TASKFLOW_CRASH_ACTION_ID,
            params = buildCrashParams(
                scene = scene,
                throwable = throwable,
                pagePath = pagePath,
                fatal = fatal,
            ),
            throwable = throwable,
        )
    }

    /**
     * ANR 上报（主线程阻塞等）；与 [reportCrash] 共用本地存储与平台扩展位。
     */
    fun reportAnr(threadDump: String) {
        val synthetic = AnrReportException(threadDump.take(ANR_MESSAGE_MAX_LENGTH))
        val pagePath = LocalLogStore.lastKnownPageId().orEmpty()
        ReleaseObservabilityContract.emit(
            channel = Channel.CRASH,
            eventOrMetric = ReleaseObservabilityContract.EVENT_ANR,
            pageId = TASKFLOW_CRASH_PAGE_ID,
            actionId = ReleaseObservabilityContract.ACTION_APP_ANR,
            params = buildCrashParams(
                scene = "anr",
                throwable = synthetic,
                pagePath = pagePath,
                fatal = true,
                threadDumpLength = threadDump.length,
            ),
            throwable = synthetic,
        )
    }

    private fun buildCrashParams(
        scene: String,
        throwable: Throwable,
        pagePath: String,
        fatal: Boolean,
        threadDumpLength: Int? = null,
    ): Map<String, String> {
        return buildMap {
            put("scene", scene)
            put("fatal", fatal.toString())
            put("type", throwable::class.simpleName.orEmpty())
            put("message", throwable.message.nullIfBlank().orEmpty())
            put("pagePath", pagePath)
            threadDumpLength?.let { length -> put("dumpLength", length.toString()) }
        }
    }

    private class AnrReportException(message: String) : RuntimeException(message)
}
