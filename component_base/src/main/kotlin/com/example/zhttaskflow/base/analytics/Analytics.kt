package com.example.zhttaskflow.base.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import com.example.zhttaskflow.base.observability.DeveloperObservability

/**
 * 产品埋点抽象层：与具体 SDK / 日志实现解耦，由壳工程通过 [LocalAnalytics] 注入实现。
 *
 * 业务侧优先使用 [rememberAnalytics] 或 [com.example.zhttaskflow.base.ui.extension.logUiInteraction] 等封装，
 * 禁止直接依赖 [DebugAnalytics]（调试默认实现）。
 *
 * 后续接入友盟 / 自研 SDK 时，实现本接口并在应用壳 [com.example.zhttaskflow.navigation.AppMainShell] 传入 `analyticsImpl`，
 * 或经 [AnalyticsCompositionRoot] / [com.example.zhttaskflow.base.ui.BaseScaffold] 的 `analytics` 参数注入。
 *
 * Release 默认实现见 `app` 模块 `ReleaseAnalytics`；日志与上报契约字段统一为 `pageId`、`actionId`（入参 `operationId` 映射为 `actionId`）。
 */
interface Analytics {

    /**
     * 页面曝光或参数快照更新。
     *
     * @param event [PageViewEvent.Enter] 对应进入页面；[PageViewEvent.ArgsChange] 对应参数变化（非首次进入）。
     */
    fun trackPageView(
        pageId: String,
        pageArgs: String? = null,
        event: PageViewEvent = PageViewEvent.Enter,
    )

    /** 页面离开（composition dispose / 路由弹出）。 */
    fun trackPageLeave(pageId: String)

    /**
     * 交互类埋点：点击、下拉刷新、操作成功/失败等。
     *
     * @param operationId 操作 ID，对应日志与监控契约中的 **actionId**（如 `task_list_fab_add`）
     * @param logTag 调试实现写入 Logcat 的子 Tag；产品实现可忽略
     */
    fun trackInteraction(
        action: String,//交互类型
        operationId: String,//操作唯一标识，对应埋点契约里的 `actionId`，是核心统计字段
        pageId: String? = null,//所属页面，可选
        params: Map<String, String?>? = null,//自定义业务参数
        detail: String? = null,
        logTag: String? = null,
    )
}

/**
 * 页面曝光子类型（调试日志与产品事件映射共用）。
 */
enum class PageViewEvent {
    Enter,// 进入页面
    ArgsChange,// 参数变化（非首次进入）
}

/**
 * 向下提供 [Analytics]；默认 [DebugAnalytics]（与 [LocalCrashReporter]、[LocalPerformance] 同为 `staticCompositionLocalOf`）。
 */
val LocalAnalytics = staticCompositionLocalOf<Analytics> {
    DebugAnalytics
}

/**
 * 获取当前 CompositionLocal 中的 [Analytics] 实例；默认 [DebugAnalytics]。
 */
@Composable
fun rememberAnalytics(): Analytics = LocalAnalytics.current

/**
 * 调试默认 [Analytics] 实例（壳层未注入 [analyticsImpl] 时使用）。
 */
@Composable
fun rememberDebugAnalytics(): Analytics {
    return remember { DebugAnalytics }
}

internal val AnalyticsFallback: Analytics = DebugAnalytics

/**
 * 批量注入场景专用：装配最终可用的埋点实例
 * 封装：默认降级策略 + 可观测装饰器包装 + 全局注册表生命周期同步
 */
@Composable
internal fun rememberManagedAnalytics(impl: Analytics? = null): Analytics {
    // 1. 默认降级：未传入则使用调试默认实现
    val shellAnalytics = impl ?: rememberDebugAnalytics()
    // 2. 装饰器包装：接入开发者面板动态路由能力
    val resolvedAnalytics = remember(shellAnalytics) {
        DeveloperObservability.wrapAnalytics(shellAnalytics)
    }
    // 3. 生命周期绑定：Registry 存 shell，current() 统一 resolve
    DisposableEffect(shellAnalytics) {
        AnalyticsRegistry.push(shellAnalytics)
        onDispose { AnalyticsRegistry.pop(shellAnalytics) }
    }
    return resolvedAnalytics
}


internal const val TASK_FLOW_ANALYTICS_LIST_ITEM_LOG_TAG: String = "ListItem"

internal const val TASK_FLOW_ANALYTICS_CLICK_LOG_TAG: String = "UiClick"

/**
 * 业务层扩展：核心 CTA 点击。
 */
fun Analytics.trackUiClick(
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
fun Analytics.trackUiOutcome(
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
