package com.example.zhttaskflow.base.observability

import com.example.zhttaskflow.base.exception.CRASH_ACTION_ID
import com.example.zhttaskflow.base.exception.CRASH_PAGE_ID
import com.example.zhttaskflow.core.observability.LocalLogStore
import com.example.zhttaskflow.core.util.nullIfBlank

/**
 * Release 风格崩溃 / ANR 共用逻辑：参数拼装与契约字段映射。
 */
internal object ReleaseCrashCore {

    private const val ANR_MESSAGE_MAX_LENGTH: Int = 2_048

    internal fun interface CrashEmit {
        fun emit(
            eventOrMetric: String,
            pageId: String?,
            actionId: String?,
            params: Map<String, String?>?,
            throwable: Throwable?,
        )
    }

    fun reportCrash(
        throwable: Throwable,
        fatal: Boolean,
        emit: CrashEmit,
    ) {
        val scene = if (fatal) "fatal" else "non_fatal"
        val pagePath = LocalLogStore.lastKnownPageId().orEmpty()
        emit.emit(
            eventOrMetric = LocalObservabilityEmitter.EVENT_CRASH,
            pageId = CRASH_PAGE_ID,
            actionId = CRASH_ACTION_ID,
            params = buildCrashParams(
                scene = scene,
                throwable = throwable,
                pagePath = pagePath,
                fatal = fatal,
            ),
            throwable = throwable,
        )
    }

    fun reportAnr(
        threadDump: String,
        emit: CrashEmit,
    ) {
        val synthetic = AnrReportException(threadDump.take(ANR_MESSAGE_MAX_LENGTH))
        val pagePath = LocalLogStore.lastKnownPageId().orEmpty()
        emit.emit(
            eventOrMetric = LocalObservabilityEmitter.EVENT_ANR,
            pageId = CRASH_PAGE_ID,
            actionId = LocalObservabilityEmitter.ACTION_APP_ANR,
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

    fun buildCrashParams(
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
