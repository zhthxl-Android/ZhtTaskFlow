package com.example.zhttaskflow.core.debug

import com.example.zhttaskflow.core.observability.LocalLogStore
import com.example.zhttaskflow.core.util.isDebugLoggingEnabled

/**
 * Debug 安装包专用开发者工具运行时开关（Release 构建中所有 API 均为 no-op / 固定 false）。
 *
 * 供日志深度调试面板切换可观测实现、模拟离线横幅等；不引入新依赖。
 */
object DeveloperTools {

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
        return isDebugLoggingEnabled() && simulateNetworkOffline
    }

    fun setObservabilityBackend(backend: ObservabilityBackend) {
        if (!isDebugLoggingEnabled()) {
            return
        }
        observabilityBackend = backend
    }

    fun setSimulateNetworkOffline(enabled: Boolean) {
        if (!isDebugLoggingEnabled()) {
            return
        }
        simulateNetworkOffline = enabled
    }

    /**
     * 向本地日志仓注入测试记录（埋点 / 性能 / 崩溃）。
     */
    fun injectTestLogs(
        channel: LocalLogStore.ObservabilityChannel,
        count: Int,
        pageId: String = "LogDebugPanel",
    ) {
        if (!isDebugLoggingEnabled()) {
            return
        }
        val safeCount = count.coerceIn(1, 5_000)
        val baseMs = System.currentTimeMillis()
        repeat(safeCount) { index ->
            val timestamp = baseMs - index
            val event = when (channel) {
                LocalLogStore.ObservabilityChannel.ANALYTICS ->
                    "debug_inject_analytics_$index"
                LocalLogStore.ObservabilityChannel.PERFORMANCE ->
                    "debug_inject_perf_$index"
                LocalLogStore.ObservabilityChannel.CRASH ->
                    "debug_inject_crash_$index"
            }
            val stackTrace = if (channel == LocalLogStore.ObservabilityChannel.CRASH) {
                "com.example.debug.SyntheticCrash: inject #$index\n    at debug.Injector.inject(Injector.kt:1)"
            } else {
                null
            }
            LocalLogStore.recordFromObservabilityEmit(
                channel = channel,
                eventOrMetric = event,
                pageId = pageId,
                actionId = "debug_inject",
                params = mapOf(
                    "seedIndex" to index.toString(),
                    "source" to "LogDebugPanel",
                ),
                stackTrace = stackTrace,
                deviceInfo = if (channel == LocalLogStore.ObservabilityChannel.CRASH) {
                    "debug_panel=true"
                } else {
                    null
                },
                anomaly = channel == LocalLogStore.ObservabilityChannel.CRASH,
            )
        }
    }
}
