package com.example.zhttaskflow.base.observability

import com.example.zhttaskflow.core.util.isDebugLoggingEnabled

/**
 * Debug 面板向本地可观测仓批量注入测试记录（经 [ObservabilityEmitPipeline] 落盘）。
 */
object ObservabilityDebugInjector {

    fun injectTestLogs(
        channel: LocalObservabilityEmitter.Channel,
        count: Int,
        pageId: String = "LogDebugPanel",
    ) {
        if (!isDebugLoggingEnabled()) {
            return
        }
        val safeCount = count.coerceIn(1, 5_000)
        repeat(safeCount) { index ->
            val eventOrMetric = when (channel) {
                LocalObservabilityEmitter.Channel.ANALYTICS ->
                    "debug_inject_analytics_$index"
                LocalObservabilityEmitter.Channel.PERFORMANCE ->
                    "debug_inject_perf_$index"
                LocalObservabilityEmitter.Channel.CRASH ->
                    "debug_inject_crash_$index"
            }
            val params = buildMap {
                put("seedIndex", index.toString())
                put("source", "LogDebugPanel")
                if (channel == LocalObservabilityEmitter.Channel.CRASH) {
                    put("deviceInfo", "debug_panel=true")
                }
            }
            val throwable = if (channel == LocalObservabilityEmitter.Channel.CRASH) {
                SyntheticInjectCrashException(index)
            } else {
                null
            }
            ObservabilityEmitPipeline.emit(
                channel = channel,
                eventOrMetric = eventOrMetric,
                pageId = pageId,
                actionId = "debug_inject",
                params = params,
                throwable = throwable,
            )
        }
    }

    private class SyntheticInjectCrashException(index: Int) : RuntimeException("inject #$index")
}
