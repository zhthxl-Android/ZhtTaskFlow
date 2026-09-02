package com.example.zhttaskflow.base.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember

/**
 * 产品埋点抽象层：与具体 SDK / 日志实现解耦，由壳工程通过 [LocalTaskFlowAnalytics] 注入实现。
 *
 * 业务侧优先使用 [rememberTaskFlowAnalytics] 或 [com.example.zhttaskflow.base.ui.extension.logUiInteraction] 等封装，
 * 禁止直接依赖 [TaskFlowDebugAnalytics]（调试默认实现）。
 *
 * 后续接入友盟 / 自研 SDK 时，实现本接口并在应用壳 `AppMainShell` 传入 `analyticsImpl`，
 * 或经 [TaskFlowAnalyticsCompositionRoot] / [com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold] 的 `analytics` 参数注入。
 */
interface TaskFlowAnalytics {

    /**
     * 页面曝光或参数快照更新。
     *
     * @param event [TaskFlowPageViewEvent.Enter] 对应进入页面；[TaskFlowPageViewEvent.ArgsChange] 对应参数变化（非首次进入）。
     */
    fun trackPageView(
        pageId: String,
        pageArgs: String? = null,
        event: TaskFlowPageViewEvent = TaskFlowPageViewEvent.Enter,
    )

    /** 页面离开（composition dispose / 路由弹出）。 */
    fun trackPageLeave(pageId: String)

    /**
     * 交互类埋点：点击、下拉刷新、操作成功/失败等。
     *
     * @param operationId 操作 ID（opId），如 `home_entrance_card`
     * @param logTag 调试实现写入 Logcat 的子 Tag；产品实现可忽略
     */
    fun trackInteraction(
        action: String,
        operationId: String,
        pageId: String? = null,
        params: Map<String, String?>? = null,
        detail: String? = null,
        logTag: String? = null,
    )
}

/**
 * 页面曝光子类型（调试日志与产品事件映射共用）。
 */
enum class TaskFlowPageViewEvent {
    Enter,
    ArgsChange,
}

/**
 * 向下提供 [TaskFlowAnalytics]；默认 [TaskFlowDebugAnalytics]。
 */
val LocalTaskFlowAnalytics = compositionLocalOf<TaskFlowAnalytics> {
    TaskFlowDebugAnalytics
}

@Composable
fun rememberTaskFlowAnalytics(): TaskFlowAnalytics {
    return LocalTaskFlowAnalytics.current
}

/**
 * 调试默认 [TaskFlowAnalytics] 实例（壳层未注入 [analyticsImpl] 时使用）。
 */
@Composable
fun rememberTaskFlowDebugAnalytics(): TaskFlowAnalytics {
    return remember { TaskFlowDebugAnalytics }
}

internal val TaskFlowAnalyticsFallback: TaskFlowAnalytics = TaskFlowDebugAnalytics

/**
 * 在壳层装配 Analytics，并与 [TaskFlowAnalyticsRegistry] 同步。
 *
 * 通常由 [com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold] 调用；应用壳也可在更外层包裹以提前注入。
 */
@Composable
fun TaskFlowAnalyticsCompositionRoot(
    analytics: TaskFlowAnalytics = rememberTaskFlowDebugAnalytics(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalTaskFlowAnalytics provides analytics) {
        DisposableEffect(analytics) {
            TaskFlowAnalyticsRegistry.push(analytics)
            onDispose {
                TaskFlowAnalyticsRegistry.pop(analytics)
            }
        }
        content()
    }
}

/**
 * 非 Composable 场景（如点击 lambda、[Modifier.clickWithLog]）解析当前 Analytics。
 */
internal object TaskFlowAnalyticsRegistry {

    private val stack = ArrayDeque<TaskFlowAnalytics>()

    fun push(analytics: TaskFlowAnalytics) {
        stack.addLast(analytics)
    }

    fun pop(analytics: TaskFlowAnalytics) {
        if (stack.isNotEmpty() && stack.last() === analytics) {
            stack.removeLast()
        }
    }

    fun current(): TaskFlowAnalytics {
        return stack.lastOrNull() ?: TaskFlowAnalyticsFallback
    }
}

/**
 * 交互日志单行格式（与历史交互埋点字符串一致）。
 */
internal object TaskFlowAnalyticsMessageFormatter {

    fun formatInteraction(
        action: String,
        operationId: String,
        pageId: String? = null,
        params: Map<String, String?>? = null,
        detail: String? = null,
    ): String {
        val parts = buildList {
            add("action=$action")
            if (!pageId.isNullOrBlank()) {
                add("pageId=$pageId")
            }
            add("actionId=$operationId")
            val snapshot = formatParamsSnapshot(params = params, detail = detail)
            if (snapshot.isNotBlank()) {
                add("params=$snapshot")
            }
        }
        return parts.joinToString(separator = " ")
    }

    private fun formatParamsSnapshot(
        params: Map<String, String?>?,
        detail: String?,
    ): String {
        val fromMap = params
            ?.entries
            ?.mapNotNull { (key, value) ->
                value?.let { safeValue -> "$key=$safeValue" }
            }
            ?.joinToString(separator = ",")
        return when {
            !fromMap.isNullOrBlank() && !detail.isNullOrBlank() -> "$fromMap,$detail"
            !fromMap.isNullOrBlank() -> fromMap
            !detail.isNullOrBlank() -> detail
            else -> ""
        }
    }
}

internal const val TASK_FLOW_ANALYTICS_LIST_ITEM_LOG_TAG: String = "ListItem"

internal const val TASK_FLOW_ANALYTICS_CLICK_LOG_TAG: String = "UiClick"

/**
 * 业务层扩展：核心 CTA 点击。
 */
fun TaskFlowAnalytics.trackUiClick(
    operationId: String,
    pageId: String? = null,
    params: Map<String, String?>? = null,
    detail: String? = null,
    logTag: String? = TASK_FLOW_ANALYTICS_CLICK_LOG_TAG,
) {
    trackInteraction(
        action = "click",
        operationId = operationId,
        pageId = pageId,
        params = params,
        detail = detail,
        logTag = logTag,
    )
}

/**
 * 业务层扩展：操作结果（success / failure / info）。
 */
fun TaskFlowAnalytics.trackUiOutcome(
    outcome: String,
    operationId: String,
    pageId: String? = null,
    params: Map<String, String?>? = null,
) {
    trackInteraction(
        action = outcome,
        operationId = operationId,
        pageId = pageId,
        params = params,
        logTag = null,
    )
}
