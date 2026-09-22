package com.example.zhttaskflow.base.exception

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.zhttaskflow.base.observability.DeveloperObservability

const val CRASH_LOG_TAG: String = "Exception"
const val CRASH_PAGE_ID: String = "AppShell"
const val CRASH_ACTION_ID: String = "app_uncaught_crash"

/**
 * 崩溃 / 未捕获异常上报抽象：产品环境由壳工程注入 Bugly、Crashlytics 等实现。
 *
 * - 壳层注入：[com.example.zhttaskflow.navigation.AppMainShell] 的 `crashReporterImpl` →
 *   [com.example.zhttaskflow.base.ui.BaseScaffold] → [com.example.zhttaskflow.base.exception.ExceptionMonitoringRoot]。
 * - CompositionLocal：[LocalCrashReporter]（`staticCompositionLocalOf`）；非 Composable 场景经 [CrashReporterRegistry] 解析。
 * - Release 默认：`app` 模块 `ReleaseCrashReporter`（契约 `actionId` 见 [CRASH_ACTION_ID] / `app_anr`）。
 * - 调试默认：[DebugCrashReporter]（`Logger` + Analytics outcome，`actionId=[CRASH_ACTION_ID]`）。
 *
 * ANR：Release 在 [com.example.zhttaskflow.TaskFlowApplication] 调用 `ReleaseAnrMonitor.install`；
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

//默认值为DebugCrashReporter
val LocalCrashReporter = staticCompositionLocalOf<CrashReporter> {
    DebugCrashReporter
}

//保底上报器
internal val CrashReporterFallback: CrashReporter = DebugCrashReporter

/**
 * 获取当前 CompositionLocal 中的 [CrashReporter]；[override] 非空时优先于 [LocalCrashReporter] 默认值。
 */
@Composable
fun rememberCrashReporter(override: CrashReporter? = null): CrashReporter {
    val fromLocal = LocalCrashReporter.current
    return override ?: fromLocal
}

/**
 * 批量注入场景专用：装配最终可用的崩溃上报实例
 * 封装：默认降级策略 + 可观测装饰器包装 + 全局注册表生命周期同步
 */
@Composable
internal fun rememberManagedCrashReporter(impl: CrashReporter? = null): CrashReporter {
    // 1. 默认降级：未传入则使用 CompositionLocal 默认值
    val shellReporter = rememberCrashReporter(override = impl)
    // 2. 装饰器包装：接入开发者面板动态路由能力
    val resolvedReporter = remember(shellReporter) {
        DeveloperObservability.wrapCrashReporter(shellReporter)
    }
    // 3. 生命周期绑定：Registry 存 shell，current() 统一 resolve
    DisposableEffect(shellReporter) {
        CrashReporterRegistry.push(shellReporter)
        onDispose { CrashReporterRegistry.pop(shellReporter) }
    }
    return resolvedReporter
}

