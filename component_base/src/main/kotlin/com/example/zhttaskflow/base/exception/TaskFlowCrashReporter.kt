package com.example.zhttaskflow.base.exception

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.example.zhttaskflow.base.analytics.TaskFlowAnalyticsRegistry
import com.example.zhttaskflow.base.analytics.trackUiOutcome
import com.example.zhttaskflow.core.log.TaskFlowLogger
import com.example.zhttaskflow.core.util.nullIfBlank

const val TASKFLOW_CRASH_LOG_TAG: String = "Exception"
const val TASKFLOW_CRASH_PAGE_ID: String = "AppShell"
const val TASKFLOW_CRASH_ACTION_ID: String = "app_uncaught_crash"

/**
 * 崩溃 / 未捕获异常上报抽象：产品环境由壳工程注入 Bugly、Crashlytics 等实现。
 *
 * - 壳层注入：[com.example.zhttaskflow.navigation.AppMainShell] 的 `crashReporterImpl` →
 *   [com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold] → [com.example.zhttaskflow.base.exception.TaskFlowExceptionMonitoringRoot]。
 * - CompositionLocal：[LocalTaskFlowCrashReporter]；非 Composable 场景经 [TaskFlowCrashReporterRegistry] 解析。
 * - Release 默认：`app` 模块 `ReleaseTaskFlowCrashReporter`（契约 `actionId` 见 [TASKFLOW_CRASH_ACTION_ID] / `app_anr`）。
 * - 调试默认：[TaskFlowDebugCrashReporter]（`TaskFlowLogger` + Analytics outcome，`actionId=[TASKFLOW_CRASH_ACTION_ID]`）。
 *
 * ANR：Release 在 [com.example.zhttaskflow.TaskFlowApplication] 调用 `ReleaseTaskFlowCrashMonitoring.install`；
 * 产品 SDK 接入后可在同一初始化点替换轻量探测。
 */
fun interface TaskFlowCrashReporter {

    /**
     * @param fatal `true` 表示进程级未捕获崩溃；`false` 表示协程等可恢复未捕获异常。
     */
    fun reportCrash(throwable: Throwable, fatal: Boolean)
}

/**
 * 调试默认上报：[TaskFlowLogger.errorAlways] + Analytics outcome（`pageId=[TASKFLOW_CRASH_PAGE_ID]`、`actionId=[TASKFLOW_CRASH_ACTION_ID]`）。
 */
object TaskFlowDebugCrashReporter : TaskFlowCrashReporter {

    override fun reportCrash(throwable: Throwable, fatal: Boolean) {
        val scene = if (fatal) "fatal" else "non_fatal"
        TaskFlowLogger.errorAlways(TASKFLOW_CRASH_LOG_TAG, {
            "[$scene] ${throwable.message.nullIfBlank() ?: throwable::class.simpleName.orEmpty()}"
        }, throwable)
        TaskFlowAnalyticsRegistry.current().trackUiOutcome(
            outcome = "failure",
            operationId = TASKFLOW_CRASH_ACTION_ID,
            pageId = TASKFLOW_CRASH_PAGE_ID,
            params = mapOf(
                "fatal" to fatal.toString(),
                "type" to throwable::class.simpleName.orEmpty(),
            ),
        )
    }
}

val LocalTaskFlowCrashReporter = staticCompositionLocalOf<TaskFlowCrashReporter> {
    TaskFlowDebugCrashReporter
}

internal val TaskFlowCrashReporterFallback: TaskFlowCrashReporter = TaskFlowDebugCrashReporter

/**
 * 解析当前组合树或壳层注入的崩溃上报实现；未注入时与 [LocalTaskFlowCrashReporter] 默认一致。
 */
@Composable
fun rememberTaskFlowCrashReporter(
    override: TaskFlowCrashReporter? = null,
): TaskFlowCrashReporter {
    val fromLocal = LocalTaskFlowCrashReporter.current
    return remember(override, fromLocal) {
        override ?: fromLocal
    }
}

/**
 * 装配崩溃上报并与 [TaskFlowCrashReporterRegistry] 同步（供非 Composable 的
 * [com.example.zhttaskflow.base.exception.TaskFlowExceptionHandler.reportCrash] 解析）。
 *
 * 由 [com.example.zhttaskflow.base.exception.TaskFlowExceptionMonitoringRoot] 在拥有全局宿主时调用；
 * 与网络离线横幅、未捕获异常钩子同层装配。
 */
@Composable
fun TaskFlowCrashReporterCompositionRoot(
    crashReporter: TaskFlowCrashReporter = rememberTaskFlowCrashReporter(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalTaskFlowCrashReporter provides crashReporter) {
        DisposableEffect(crashReporter) {
            TaskFlowCrashReporterRegistry.push(crashReporter)
            onDispose {
                TaskFlowCrashReporterRegistry.pop(crashReporter)
            }
        }
        content()
    }
}

/**
 * 非 Composable 场景（未捕获异常钩子、[TaskFlowExceptionHandler.reportCrash]）解析当前 Reporter。
 */
internal object TaskFlowCrashReporterRegistry {

    private val stack = ArrayDeque<TaskFlowCrashReporter>()

    fun push(reporter: TaskFlowCrashReporter) {
        stack.addLast(reporter)
    }

    fun pop(reporter: TaskFlowCrashReporter) {
        if (stack.isNotEmpty() && stack.last() === reporter) {
            stack.removeLast()
        }
    }

    fun current(): TaskFlowCrashReporter {
        return stack.lastOrNull() ?: TaskFlowCrashReporterFallback
    }
}
