package com.example.zhttaskflow.core.debug

import com.example.zhttaskflow.core.observability.TaskFlowLocalLogStore
import com.example.zhttaskflow.core.util.isTaskFlowDebugLoggingEnabled

/**
 * Debug 安装包专用开发者工具运行时开关（Release 构建中所有 API 均为 no-op / 固定 false）。
 *
 * 供日志深度调试面板切换可观测实现、模拟离线横幅等；不引入新依赖。
 */
object TaskFlowDeveloperTools {

    /**
     * 可观测后端：与壳层默认 Debug 实现相对，[RELEASE_LOCAL] 走本地落盘 + Logcat（对齐 Release 契约字段）。
     */
    enum class ObservabilityBackend {
        /** 使用壳层注入的 Debug / Release 默认实现（经 Registry / CompositionLocal）。 */
        APP_SHELL_DEFAULT,

        /** 强制使用「Release 风格」本地仓写入（仅 Debug 包可调）。 */
        RELEASE_LOCAL,
    }

    @Volatile
    var observabilityBackend: ObservabilityBackend = ObservabilityBackend.APP_SHELL_DEFAULT
        private set

    @Volatile
    var simulateNetworkOffline: Boolean = false
        private set

    /**
     * 是否应展示全局离线横幅（Debug 模拟优先于系统网络态）。
     */
    fun shouldForceOfflineBanner(): Boolean {
        return isTaskFlowDebugLoggingEnabled() && simulateNetworkOffline
    }

    fun setObservabilityBackend(backend: ObservabilityBackend) {
        if (!isTaskFlowDebugLoggingEnabled()) {
            return
        }
        observabilityBackend = backend
    }

    fun setSimulateNetworkOffline(enabled: Boolean) {
        if (!isTaskFlowDebugLoggingEnabled()) {
            return
        }
        simulateNetworkOffline = enabled
    }

    /**
     * 向本地日志仓注入测试记录（埋点 / 性能 / 崩溃）。
     */
    fun injectTestLogs(
        channel: TaskFlowLocalLogStore.ObservabilityChannel,
        count: Int,
        pageId: String = "LogDebugPanel",
    ) {
        if (!isTaskFlowDebugLoggingEnabled()) {
            return
        }
        val safeCount = count.coerceIn(1, 5_000)
        val baseMs = System.currentTimeMillis()
        repeat(safeCount) { index ->
            val timestamp = baseMs - index
            val event = when (channel) {
                TaskFlowLocalLogStore.ObservabilityChannel.ANALYTICS ->
                    "debug_inject_analytics_$index"
                TaskFlowLocalLogStore.ObservabilityChannel.PERFORMANCE ->
                    "debug_inject_perf_$index"
                TaskFlowLocalLogStore.ObservabilityChannel.CRASH ->
                    "debug_inject_crash_$index"
            }
            val stackTrace = if (channel == TaskFlowLocalLogStore.ObservabilityChannel.CRASH) {
                "com.example.debug.SyntheticCrash: inject #$index\n    at debug.Injector.inject(Injector.kt:1)"
            } else {
                null
            }
            TaskFlowLocalLogStore.recordFromObservabilityEmit(
                channel = channel,
                eventOrMetric = event,
                pageId = pageId,
                actionId = "debug_inject",
                params = mapOf(
                    "seedIndex" to index.toString(),
                    "source" to "LogDebugPanel",
                ),
                stackTrace = stackTrace,
                deviceInfo = if (channel == TaskFlowLocalLogStore.ObservabilityChannel.CRASH) {
                    "debug_panel=true"
                } else {
                    null
                },
                anomaly = channel == TaskFlowLocalLogStore.ObservabilityChannel.CRASH,
            )
        }
    }
}
