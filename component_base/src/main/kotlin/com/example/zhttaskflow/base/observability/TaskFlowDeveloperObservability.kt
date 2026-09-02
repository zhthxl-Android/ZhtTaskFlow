package com.example.zhttaskflow.base.observability

import com.example.zhttaskflow.base.analytics.TaskFlowAnalytics
import com.example.zhttaskflow.base.analytics.TaskFlowPageViewEvent
import com.example.zhttaskflow.base.exception.TASKFLOW_CRASH_ACTION_ID
import com.example.zhttaskflow.base.exception.TASKFLOW_CRASH_PAGE_ID
import com.example.zhttaskflow.base.exception.TaskFlowCrashReporter
import com.example.zhttaskflow.base.performance.TaskFlowPerformanceReporter
import com.example.zhttaskflow.core.debug.TaskFlowDeveloperTools
import com.example.zhttaskflow.core.observability.TaskFlowLocalLogStore
import com.example.zhttaskflow.core.util.isTaskFlowDebugLoggingEnabled
import com.example.zhttaskflow.core.util.nullIfBlank

/**
 * Debug 包深度调试面板：在壳层默认实现与 Release 风格本地落盘之间路由可观测三联。
 */
object TaskFlowDeveloperObservability {

    fun resolveAnalytics(shellDefault: TaskFlowAnalytics): TaskFlowAnalytics {
        if (!isTaskFlowDebugLoggingEnabled()) {
            return shellDefault
        }
        return when (TaskFlowDeveloperTools.observabilityBackend) {
            TaskFlowDeveloperTools.ObservabilityBackend.RELEASE_LOCAL -> TaskFlowReleaseLocalAnalytics
            TaskFlowDeveloperTools.ObservabilityBackend.APP_SHELL_DEFAULT -> shellDefault
        }
    }

    fun wrapAnalytics(shellDefault: TaskFlowAnalytics): TaskFlowAnalytics {
        return object : TaskFlowAnalytics {
            override fun trackPageView(
                pageId: String,
                pageArgs: String?,
                event: TaskFlowPageViewEvent,
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

    fun wrapCrashReporter(shellDefault: TaskFlowCrashReporter): TaskFlowCrashReporter {
        return TaskFlowCrashReporter { throwable, fatal ->
            resolveCrashReporter(shellDefault).reportCrash(throwable, fatal)
        }
    }

    fun resolveCrashReporter(shellDefault: TaskFlowCrashReporter): TaskFlowCrashReporter {
        if (!isTaskFlowDebugLoggingEnabled()) {
            return shellDefault
        }
        return when (TaskFlowDeveloperTools.observabilityBackend) {
            TaskFlowDeveloperTools.ObservabilityBackend.RELEASE_LOCAL -> TaskFlowReleaseLocalCrashReporter
            TaskFlowDeveloperTools.ObservabilityBackend.APP_SHELL_DEFAULT -> shellDefault
        }
    }

    fun wrapPerformanceReporter(shellDefault: TaskFlowPerformanceReporter): TaskFlowPerformanceReporter {
        return object : TaskFlowPerformanceReporter {
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

    private fun resolvePerformanceReporter(shellDefault: TaskFlowPerformanceReporter): TaskFlowPerformanceReporter {
        if (!isTaskFlowDebugLoggingEnabled()) {
            return shellDefault
        }
        return when (TaskFlowDeveloperTools.observabilityBackend) {
            TaskFlowDeveloperTools.ObservabilityBackend.RELEASE_LOCAL -> TaskFlowReleaseLocalPerformanceReporter
            TaskFlowDeveloperTools.ObservabilityBackend.APP_SHELL_DEFAULT -> shellDefault
        }
    }

    /**
     * 模拟 ANR 上报（写入本地仓，不阻塞主线程）。
     */
    fun simulateAnrReport(threadDump: String = "Debug panel synthetic ANR thread dump") {
        if (!isTaskFlowDebugLoggingEnabled()) {
            return
        }
        TaskFlowReleaseLocalCrashReporter.reportAnr(threadDump)
    }

    /**
     * 模拟慢函数：上报性能指标并可选短暂阻塞主线程（用于验证卡顿监控）。
     */
    fun simulateSlowFunction(blockMainThreadMs: Long = 0L) {
        if (!isTaskFlowDebugLoggingEnabled()) {
            return
        }
        val reporter = resolvePerformanceReporter(TaskFlowReleaseLocalPerformanceReporter)
        val start = System.currentTimeMillis()
        if (blockMainThreadMs > 0L) {
            Thread.sleep(blockMainThreadMs.coerceAtMost(8_000L))
        }
        val elapsed = System.currentTimeMillis() - start
        reporter.onFirstFrameRendered(
            pageId = "LogDebugPanel",
            durationMs = elapsed,
        )
        TaskFlowLocalObservabilityEmitter.emit(
            channel = TaskFlowLocalObservabilityEmitter.Channel.PERFORMANCE,
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
        if (!isTaskFlowDebugLoggingEnabled()) {
            return
        }
        val error = IllegalStateException(message)
        resolveCrashReporter(TaskFlowReleaseLocalCrashReporter).reportCrash(error, fatal = false)
    }
}

private object TaskFlowReleaseLocalAnalytics : TaskFlowAnalytics {

    override fun trackPageView(
        pageId: String,
        pageArgs: String?,
        event: TaskFlowPageViewEvent,
    ) {
        if (event == TaskFlowPageViewEvent.Enter || event == TaskFlowPageViewEvent.ArgsChange) {
            TaskFlowLocalLogStore.updateLastKnownPageId(pageId)
        }
        val actionId = when (event) {
            TaskFlowPageViewEvent.Enter -> TaskFlowLocalObservabilityEmitter.ACTION_PAGE_ENTER
            TaskFlowPageViewEvent.ArgsChange -> TaskFlowLocalObservabilityEmitter.ACTION_PAGE_ARGS_CHANGE
        }
        val params = buildMap {
            put("lifecycle", event.name)
            if (!pageArgs.isNullOrBlank()) {
                put("pageArgs", pageArgs)
            }
        }
        TaskFlowLocalObservabilityEmitter.emit(
            channel = TaskFlowLocalObservabilityEmitter.Channel.ANALYTICS,
            eventOrMetric = actionId,
            pageId = pageId,
            actionId = actionId,
            params = params,
        )
    }

    override fun trackPageLeave(pageId: String) {
        TaskFlowLocalObservabilityEmitter.emit(
            channel = TaskFlowLocalObservabilityEmitter.Channel.ANALYTICS,
            eventOrMetric = TaskFlowLocalObservabilityEmitter.ACTION_PAGE_LEAVE,
            pageId = pageId,
            actionId = TaskFlowLocalObservabilityEmitter.ACTION_PAGE_LEAVE,
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
        TaskFlowLocalObservabilityEmitter.emit(
            channel = TaskFlowLocalObservabilityEmitter.Channel.ANALYTICS,
            eventOrMetric = "ui_interaction",
            pageId = pageId,
            actionId = operationId,
            params = mergedParams,
        )
    }
}

private object TaskFlowReleaseLocalPerformanceReporter : TaskFlowPerformanceReporter {

    override fun onFirstFrameRendered(pageId: String, durationMs: Long) {
        TaskFlowLocalObservabilityEmitter.emit(
            channel = TaskFlowLocalObservabilityEmitter.Channel.PERFORMANCE,
            eventOrMetric = TaskFlowLocalObservabilityEmitter.METRIC_FIRST_FRAME,
            pageId = pageId,
            actionId = TaskFlowLocalObservabilityEmitter.METRIC_FIRST_FRAME,
            params = mapOf("durationMs" to durationMs.toString()),
        )
    }

    override fun onScrollFpsSample(pageId: String, fps: Float, frameCount: Int) {
        TaskFlowLocalObservabilityEmitter.emit(
            channel = TaskFlowLocalObservabilityEmitter.Channel.PERFORMANCE,
            eventOrMetric = TaskFlowLocalObservabilityEmitter.METRIC_SCROLL_FPS,
            pageId = pageId,
            actionId = TaskFlowLocalObservabilityEmitter.METRIC_SCROLL_FPS,
            params = mapOf(
                "fps" to "%.1f".format(fps),
                "frameCount" to frameCount.toString(),
            ),
        )
    }

    override fun onPageDwell(pageId: String, dwellMs: Long) {
        TaskFlowLocalObservabilityEmitter.emit(
            channel = TaskFlowLocalObservabilityEmitter.Channel.PERFORMANCE,
            eventOrMetric = TaskFlowLocalObservabilityEmitter.METRIC_PAGE_DWELL,
            pageId = pageId,
            actionId = TaskFlowLocalObservabilityEmitter.METRIC_PAGE_DWELL,
            params = mapOf("dwellMs" to dwellMs.toString()),
        )
    }
}

private object TaskFlowReleaseLocalCrashReporter : TaskFlowCrashReporter {

    private const val ANR_MESSAGE_MAX_LENGTH: Int = 2_048

    override fun reportCrash(throwable: Throwable, fatal: Boolean) {
        val scene = if (fatal) "fatal" else "non_fatal"
        val pagePath = TaskFlowLocalLogStore.lastKnownPageId().orEmpty()
        TaskFlowLocalObservabilityEmitter.emit(
            channel = TaskFlowLocalObservabilityEmitter.Channel.CRASH,
            eventOrMetric = TaskFlowLocalObservabilityEmitter.EVENT_CRASH,
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

    fun reportAnr(threadDump: String) {
        val synthetic = AnrReportException(threadDump.take(ANR_MESSAGE_MAX_LENGTH))
        val pagePath = TaskFlowLocalLogStore.lastKnownPageId().orEmpty()
        TaskFlowLocalObservabilityEmitter.emit(
            channel = TaskFlowLocalObservabilityEmitter.Channel.CRASH,
            eventOrMetric = TaskFlowLocalObservabilityEmitter.EVENT_ANR,
            pageId = TASKFLOW_CRASH_PAGE_ID,
            actionId = TaskFlowLocalObservabilityEmitter.ACTION_APP_ANR,
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
