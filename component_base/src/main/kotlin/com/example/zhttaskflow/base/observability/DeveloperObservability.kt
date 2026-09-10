package com.example.zhttaskflow.base.observability

import com.example.zhttaskflow.base.analytics.Analytics
import com.example.zhttaskflow.base.analytics.PageViewEvent
import com.example.zhttaskflow.base.exception.CRASH_ACTION_ID
import com.example.zhttaskflow.base.exception.CRASH_PAGE_ID
import com.example.zhttaskflow.base.exception.CrashReporter
import com.example.zhttaskflow.base.performance.PerformanceReporter
import com.example.zhttaskflow.core.debug.DeveloperTools
import com.example.zhttaskflow.core.observability.LocalLogStore
import com.example.zhttaskflow.core.util.isDebugLoggingEnabled
import com.example.zhttaskflow.core.util.nullIfBlank

/**
 * Debug 包深度调试面板：在壳层默认实现与 Release 风格本地落盘之间路由可观测三联。
 */
object DeveloperObservability {

    fun resolveAnalytics(shellDefault: Analytics): Analytics {
        if (!isDebugLoggingEnabled()) {
            return shellDefault
        }
        return when (DeveloperTools.observabilityBackend) {
            DeveloperTools.ObservabilityBackend.RELEASE_LOCAL -> ReleaseLocalAnalytics
            DeveloperTools.ObservabilityBackend.APP_SHELL_DEFAULT -> shellDefault
        }
    }

    fun wrapAnalytics(shellDefault: Analytics): Analytics {
        return object : Analytics {
            override fun trackPageView(
                pageId: String,
                pageArgs: String?,
                event: PageViewEvent,
            ) {
                resolveAnalytics(shellDefault).trackPageView(pageId, pageArgs, event)
            }

            override fun trackPageLeave(pageId: String) {
                resolveAnalytics(shellDefault).trackPageLeave(pageId)
            }

            override fun trackInteraction(
                action: String,
                operationId: String,
                pageId: String?,
                params: Map<String, String?>?,
                detail: String?,
                logTag: String?,
            ) {
                resolveAnalytics(shellDefault).trackInteraction(
                    action,
                    operationId,
                    pageId,
                    params,
                    detail,
                    logTag,
                )
            }
        }
    }

    fun wrapCrashReporter(shellDefault: CrashReporter): CrashReporter {
        return CrashReporter { throwable, fatal ->
            resolveCrashReporter(shellDefault).reportCrash(throwable, fatal)
        }
    }

    fun resolveCrashReporter(shellDefault: CrashReporter): CrashReporter {
        if (!isDebugLoggingEnabled()) {
            return shellDefault
        }
        return when (DeveloperTools.observabilityBackend) {
            DeveloperTools.ObservabilityBackend.RELEASE_LOCAL -> ReleaseLocalCrashReporter
            DeveloperTools.ObservabilityBackend.APP_SHELL_DEFAULT -> shellDefault
        }
    }

    fun wrapPerformanceReporter(shellDefault: PerformanceReporter): PerformanceReporter {
        return object : PerformanceReporter {
            override fun onFirstFrameRendered(pageId: String, durationMs: Long) {
                resolvePerformanceReporter(shellDefault).onFirstFrameRendered(pageId, durationMs)
            }

            override fun onScrollFpsSample(pageId: String, fps: Float, frameCount: Int) {
                resolvePerformanceReporter(shellDefault).onScrollFpsSample(pageId, fps, frameCount)
            }

            override fun onPageDwell(pageId: String, dwellMs: Long) {
                resolvePerformanceReporter(shellDefault).onPageDwell(pageId, dwellMs)
            }
        }
    }

    private fun resolvePerformanceReporter(shellDefault: PerformanceReporter): PerformanceReporter {
        if (!isDebugLoggingEnabled()) {
            return shellDefault
        }
        return when (DeveloperTools.observabilityBackend) {
            DeveloperTools.ObservabilityBackend.RELEASE_LOCAL -> ReleaseLocalPerformanceReporter
            DeveloperTools.ObservabilityBackend.APP_SHELL_DEFAULT -> shellDefault
        }
    }

    /**
     * 模拟 ANR 上报（写入本地仓，不阻塞主线程）。
     */
    fun simulateAnrReport(threadDump: String = "Debug panel synthetic ANR thread dump") {
        if (!isDebugLoggingEnabled()) {
            return
        }
        ReleaseLocalCrashReporter.reportAnr(threadDump)
    }

    /**
     * 模拟慢函数：上报性能指标并可选短暂阻塞主线程（用于验证卡顿监控）。
     */
    fun simulateSlowFunction(blockMainThreadMs: Long = 0L) {
        if (!isDebugLoggingEnabled()) {
            return
        }
        val reporter = resolvePerformanceReporter(ReleaseLocalPerformanceReporter)
        val start = System.currentTimeMillis()
        if (blockMainThreadMs > 0L) {
            Thread.sleep(blockMainThreadMs.coerceAtMost(8_000L))
        }
        val elapsed = System.currentTimeMillis() - start
        reporter.onFirstFrameRendered(
            pageId = "LogDebugPanel",
            durationMs = elapsed,
        )
        LocalObservabilityEmitter.emit(
            channel = LocalObservabilityEmitter.Channel.PERFORMANCE,
            eventOrMetric = "debug_slow_function",
            pageId = "LogDebugPanel",
            actionId = "slow_function",
            params = mapOf(
                "blockedMs" to elapsed.toString(),
                "requestedBlockMs" to blockMainThreadMs.toString(),
            ),
        )
    }

    /**
     * 非致命崩溃上报（走当前可观测后端）。
     */
    fun simulateNonFatalCrash(message: String = "Debug panel synthetic crash") {
        if (!isDebugLoggingEnabled()) {
            return
        }
        val error = IllegalStateException(message)
        resolveCrashReporter(ReleaseLocalCrashReporter).reportCrash(error, fatal = false)
    }
}

private object ReleaseLocalAnalytics : Analytics {

    override fun trackPageView(
        pageId: String,
        pageArgs: String?,
        event: PageViewEvent,
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
        LocalObservabilityEmitter.emit(
            channel = LocalObservabilityEmitter.Channel.ANALYTICS,
            eventOrMetric = actionId,
            pageId = pageId,
            actionId = actionId,
            params = params,
        )
    }

    override fun trackPageLeave(pageId: String) {
        LocalObservabilityEmitter.emit(
            channel = LocalObservabilityEmitter.Channel.ANALYTICS,
            eventOrMetric = LocalObservabilityEmitter.ACTION_PAGE_LEAVE,
            pageId = pageId,
            actionId = LocalObservabilityEmitter.ACTION_PAGE_LEAVE,
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
        LocalObservabilityEmitter.emit(
            channel = LocalObservabilityEmitter.Channel.ANALYTICS,
            eventOrMetric = "ui_interaction",
            pageId = pageId,
            actionId = operationId,
            params = mergedParams,
        )
    }
}

private object ReleaseLocalPerformanceReporter : PerformanceReporter {

    override fun onFirstFrameRendered(pageId: String, durationMs: Long) {
        LocalObservabilityEmitter.emit(
            channel = LocalObservabilityEmitter.Channel.PERFORMANCE,
            eventOrMetric = LocalObservabilityEmitter.METRIC_FIRST_FRAME,
            pageId = pageId,
            actionId = LocalObservabilityEmitter.METRIC_FIRST_FRAME,
            params = mapOf("durationMs" to durationMs.toString()),
        )
    }

    override fun onScrollFpsSample(pageId: String, fps: Float, frameCount: Int) {
        LocalObservabilityEmitter.emit(
            channel = LocalObservabilityEmitter.Channel.PERFORMANCE,
            eventOrMetric = LocalObservabilityEmitter.METRIC_SCROLL_FPS,
            pageId = pageId,
            actionId = LocalObservabilityEmitter.METRIC_SCROLL_FPS,
            params = mapOf(
                "fps" to "%.1f".format(fps),
                "frameCount" to frameCount.toString(),
            ),
        )
    }

    override fun onPageDwell(pageId: String, dwellMs: Long) {
        LocalObservabilityEmitter.emit(
            channel = LocalObservabilityEmitter.Channel.PERFORMANCE,
            eventOrMetric = LocalObservabilityEmitter.METRIC_PAGE_DWELL,
            pageId = pageId,
            actionId = LocalObservabilityEmitter.METRIC_PAGE_DWELL,
            params = mapOf("dwellMs" to dwellMs.toString()),
        )
    }
}

private object ReleaseLocalCrashReporter : CrashReporter {

    private const val ANR_MESSAGE_MAX_LENGTH: Int = 2_048

    override fun reportCrash(throwable: Throwable, fatal: Boolean) {
        val scene = if (fatal) "fatal" else "non_fatal"
        val pagePath = LocalLogStore.lastKnownPageId().orEmpty()
        LocalObservabilityEmitter.emit(
            channel = LocalObservabilityEmitter.Channel.CRASH,
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

    fun reportAnr(threadDump: String) {
        val synthetic = AnrReportException(threadDump.take(ANR_MESSAGE_MAX_LENGTH))
        val pagePath = LocalLogStore.lastKnownPageId().orEmpty()
        LocalObservabilityEmitter.emit(
            channel = LocalObservabilityEmitter.Channel.CRASH,
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
