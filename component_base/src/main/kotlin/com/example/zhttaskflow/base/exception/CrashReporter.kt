package com.example.zhttaskflow.base.exception

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

const val CRASH_LOG_TAG: String = "Exception"
const val CRASH_PAGE_ID: String = "AppShell"
const val CRASH_ACTION_ID: String = "app_uncaught_crash"

/**
 * 崩溃 / 未捕获异常上报抽象：产品环境由壳工程注入 Bugly、Crashlytics 等实现。
 *
 * - 壳层注入：[com.example.zhttaskflow.navigation.AppMainShell] 的 `crashReporterImpl` →
 *   [com.example.zhttaskflow.base.ui.BaseScaffold] → [com.example.zhttaskflow.base.exception.ExceptionMonitoringRoot]。
 * - CompositionLocal：[LocalCrashReporter]；非 Composable 场景经 [CrashReporterRegistry] 解析。
 * - Release 默认：`app` 模块 `ReleaseCrashReporter`（契约 `actionId` 见 [CRASH_ACTION_ID] / `app_anr`）。
 * - 调试默认：[DebugCrashReporter]（`Logger` + Analytics outcome，`actionId=[CRASH_ACTION_ID]`）。
 *
 * ANR：Release 在 [com.example.zhttaskflow.TaskFlowApplication] 调用 `ReleaseCrashMonitoring.install`；
 * 协程未捕获异常：同 Application 内 [AppCoroutineExceptionHandler.install]。
 * 产品 SDK 接入后可在同一初始化点替换轻量探测。
 */
fun interface CrashReporter {

    /**
     * 上报崩溃 / 未捕获异常。
     * @param fatal `true` 表示进程级未捕获崩溃；
     *              `false` 表示可恢复未捕获异常（协程异常、业务捕获的 Error）。
     */
    fun reportCrash(throwable: Throwable, fatal: Boolean)
}

val LocalCrashReporter = staticCompositionLocalOf<CrashReporter> {
    DebugCrashReporter
}

internal val CrashReporterFallback: CrashReporter = DebugCrashReporter

/**
 * 解析当前组合树或壳层注入的崩溃上报实现；未注入时与 [LocalCrashReporter] 默认一致。
 */
@Composable
fun rememberCrashReporter(
    override: CrashReporter? = null,
): CrashReporter {
    val fromLocal = LocalCrashReporter.current
    return remember(override, fromLocal) {
        override ?: fromLocal
    }
}

/**
 * 装配崩溃上报并与 [CrashReporterRegistry] 同步（供非 Composable 的
 * [com.example.zhttaskflow.base.exception.ExceptionHandler.reportCrash] 解析）。
 *
 * 由 [com.example.zhttaskflow.base.exception.ExceptionMonitoringRoot] 在拥有全局宿主时调用；
 * 与网络离线横幅、未捕获异常钩子同层装配。
 */
@Composable
fun CrashReporterCompositionRoot(
    crashReporter: CrashReporter = rememberCrashReporter(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalCrashReporter provides crashReporter) {
        DisposableEffect(crashReporter) {
            CrashReporterRegistry.push(crashReporter)
            onDispose {
                CrashReporterRegistry.pop(crashReporter)
            }
        }
        content()
    }
}
