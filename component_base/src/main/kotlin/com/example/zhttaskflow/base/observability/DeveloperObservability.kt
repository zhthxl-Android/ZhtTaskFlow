package com.example.zhttaskflow.base.observability

import com.example.zhttaskflow.base.analytics.Analytics
import com.example.zhttaskflow.base.analytics.PageViewEvent
import com.example.zhttaskflow.base.analytics.ReleaseAnalytics
import com.example.zhttaskflow.base.exception.CrashReporter
import com.example.zhttaskflow.base.exception.DebugCrashReporter
import com.example.zhttaskflow.base.exception.ReleaseCrashReporter
import com.example.zhttaskflow.base.performance.PerformanceReporter
import com.example.zhttaskflow.base.performance.ReleasePerformanceReporter
import com.example.zhttaskflow.core.debug.DeveloperTools
import com.example.zhttaskflow.core.util.isDebugLoggingEnabled

/**
 * Debug 包深度调试面板：在壳层默认实现与 Release 风格本地落盘之间路由可观测三联。
 */
object DeveloperObservability {

    /**
     * 解析出「当前环境应该使用的埋点实现」
     * @param shellDefault 壳层默认实现
     * */
    fun resolveAnalytics(shellDefault: Analytics): Analytics {
        // Release 包：直接返回壳层实现，零开销
        if (!isDebugLoggingEnabled()) {
            return shellDefault
        }
        //根据面板的开关选择
        return when (DeveloperTools.observabilityBackend) {
            DeveloperTools.ObservabilityBackend.RELEASE_LOCAL -> ReleaseAnalytics
            DeveloperTools.ObservabilityBackend.APP_SHELL_DEFAULT -> shellDefault
        }
    }

    /**
     * 返回一个稳定的装饰器对象
     * @param shellDefault 壳层默认实现
     * */
    fun wrapAnalytics(shellDefault: Analytics): Analytics {
        return object : Analytics {
            override fun trackPageView(
                pageId: String,
                pageArgs: String?,
                event: PageViewEvent,
            ) {
                //每次调用埋点方法时都会重新 resolve 一次
                //面板切换后端后，下一次埋点立刻生效，不需要重建对象
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

    // 每次调用时动态判断当前该用哪个实现
    fun resolveCrashReporter(shellDefault: CrashReporter): CrashReporter {
        // Release 包：直接返回壳层实现，零开销
        if (!isDebugLoggingEnabled()) {
            return shellDefault
        }
        // Debug 包：根据开发者面板的开关切换
        return when (DeveloperTools.observabilityBackend) {
            DeveloperTools.ObservabilityBackend.RELEASE_LOCAL -> ReleaseCrashReporter
            DeveloperTools.ObservabilityBackend.APP_SHELL_DEFAULT -> shellDefault
        }
    }

    // 性能上报器的装饰器包装，每次性能回调时动态判断当前该用哪个实现
    fun wrapPerformanceReporter(shellDefault: PerformanceReporter): PerformanceReporter {
        return object : PerformanceReporter {
            override fun onFirstFrameRendered(pageId: String, durationMs: Long) {
                //首帧渲染完成上报
                resolvePerformanceReporter(shellDefault).onFirstFrameRendered(pageId, durationMs)
            }

            override fun onScrollFpsSample(pageId: String, fps: Float, frameCount: Int) {
                //滑动帧率采样上报
                resolvePerformanceReporter(shellDefault).onScrollFpsSample(pageId, fps, frameCount)
            }

            override fun onPageDwell(pageId: String, dwellMs: Long) {
                //页面停留时长上报
                resolvePerformanceReporter(shellDefault).onPageDwell(pageId, dwellMs)
            }
        }
    }

    // 解析当前应该使用的性能上报实现
    private fun resolvePerformanceReporter(shellDefault: PerformanceReporter): PerformanceReporter {
        if (!isDebugLoggingEnabled()) {
            return shellDefault
        }
        return when (DeveloperTools.observabilityBackend) {
            DeveloperTools.ObservabilityBackend.RELEASE_LOCAL -> ReleasePerformanceReporter
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
        ReleaseCrashReporter.reportAnr(threadDump)
    }

    /**
     * 模拟慢函数：上报性能指标并可选短暂阻塞主线程（用于验证卡顿监控）。
     */
    fun simulateSlowFunction(blockMainThreadMs: Long = 0L) {
        if (!isDebugLoggingEnabled()) {
            return
        }
        val start = System.currentTimeMillis()
        if (blockMainThreadMs > 0L) {
            //限制最大 8 秒
            Thread.sleep(blockMainThreadMs.coerceAtMost(8_000L))
        }
        val elapsed = System.currentTimeMillis() - start
        resolvePerformanceReporter(ReleasePerformanceReporter).onFirstFrameRendered(
            pageId = "LogDebugPanel",
            durationMs = elapsed,
        )
        ObservabilityEmitPipeline.emit(
            channel = LocalObservabilityEmitter.Channel.PERFORMANCE,
            eventOrMetric = "debug_slow_function",
            pageId = "LogDebugPanel",
            actionId = "slow_function",
            params = mapOf(
                "blockedMs" to elapsed.toString(),
                "requestedBlockMs" to blockMainThreadMs.toString(),
            ),
            throwable = null,
        )
    }

    /**
     * 模拟一次非致命崩溃上报（走当前可观测后端）。
     */
    fun simulateNonFatalCrash(message: String = "Debug panel synthetic crash") {
        if (!isDebugLoggingEnabled()) {
            return
        }
        val error = IllegalStateException(message)
        resolveCrashReporter(DebugCrashReporter).reportCrash(error, fatal = false)
    }
}
