package com.example.zhttaskflow.base.ui.extension

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import com.example.zhttaskflow.base.analytics.TASK_FLOW_ANALYTICS_CLICK_LOG_TAG
import com.example.zhttaskflow.base.analytics.TASK_FLOW_ANALYTICS_LIST_ITEM_LOG_TAG
import com.example.zhttaskflow.base.analytics.AnalyticsMessageFormatter
import com.example.zhttaskflow.base.analytics.AnalyticsRegistry

private const val UI_LONG_CLICK_LOG_TAG = "UiLongClick"
private const val UI_OUTCOME_LOG_TAG = "UiOutcome"

/**
 * Compose 层关键交互 Debug 埋点工具（与 [PageLifecycleLog] 互补）。
 *
 * 底层经 [com.example.zhttaskflow.base.analytics.Analytics] 上报，默认 [com.example.zhttaskflow.base.analytics.DebugAnalytics]，
 * 业务调用方式不变；壳层替换 [com.example.zhttaskflow.base.analytics.LocalAnalytics] 即可切换产品 SDK。
 *
 * ## 关键交互必埋点规范（团队标准）
 *
 * 每个业务页面至少覆盖以下三类日志，便于联调与线上问题还原：
 *
 * 1. **页面级曝光** — 使用 [PageLifecycleLog]：`onEnter` / `onLeave` / `onArgsChange`，
 *    `pageName` 与业务 Screen 一致（如 `LogViewer`、`TaskList`、`ArticleList`），`pageArgs` 携带列表条数、加载态等快照。
 * 2. **核心 CTA 点击** — 使用 [logUiInteraction] 或 [Modifier.clickWithLog] / [listItemClickWithLog]：
 *    `action` 为 `click`、`pullRefresh`、`loadMore` 等；`actionId`（入参 [identifier]）命名 `{page}_{控件}`
 *    （如 `task_list_fab_add`、`log_export`）；**必须**传入 [pageId]（与 [PageLifecycleLog] 的 `pageName` 对齐），
 *    业务字段写入 [params]。
 * 3. **操作成功 / 失败** — 在展示 Snackbar 等反馈处调用 [logUiOutcome] 或 [logUiInteraction]：
 *    `action` 使用 `success` / `failure` / `info`；`actionId` 如 `{page}_snackbar` 或 `{page}_{业务}_result`；
 *    [params] 携带 `message` 等（勿记录敏感信息）。
 *
 * ## 统一输出格式（与生命周期日志键值风格对齐）
 *
 * 交互单行固定包含 **pageId、actionId、params**（另附 `action` 表示交互类型）：
 *
 * `action={action} pageId={pageId} actionId={actionId} params={k1=v1,k2=v2}`
 *
 * - [pageId] 新页面 **禁止省略**；[params] 无内容时可省略 `params=` 段。
 * - [detail] 仅兼容旧调用，会合并进 `params` 快照，新代码请只用 [params]。
 *
 * @see PageLifecycleLog
 */
internal fun buildInteractionLogMessage(
    action: String,
    identifier: String,
    pageId: String? = null,
    params: Map<String, String?>? = null,
    detail: String? = null,
): String {
    return AnalyticsMessageFormatter.formatInteraction(
        action = action,
        operationId = identifier,
        pageId = pageId,
        params = params,
        detail = detail,
    )
}

/**
 * 非 [Modifier] 交互（如 [androidx.compose.material3.IconButton]、下拉刷新、加载更多、结果反馈）的统一埋点。
 *
 * @param identifier 对应日志字段 **actionId**（操作 ID）。
 */
fun logUiInteraction(
    action: String,
    identifier: String,
    pageId: String? = null,
    params: Map<String, String?>? = null,
    detail: String? = null,
    tag: String = defaultTagForAction(action),
) {
    AnalyticsRegistry.current().trackInteraction(
        action = action,
        operationId = identifier,
        pageId = pageId,
        params = params,
        detail = detail,
        logTag = tag,
    )
}

/**
 * 操作结果类埋点（Snackbar / 业务成功失败），[outcome] 为 `success` / `failure` / `info`。
 */
fun logUiOutcome(
    pageId: String,
    actionId: String,
    outcome: String,
    params: Map<String, String?>? = null,
) {
    logUiInteraction(
        action = outcome,
        identifier = actionId,
        pageId = pageId,
        params = params,
        tag = UI_OUTCOME_LOG_TAG,
    )
}

private fun defaultTagForAction(action: String): String {
    return when (action) {
        "success", "failure", "info" -> UI_OUTCOME_LOG_TAG
        "longClick" -> UI_LONG_CLICK_LOG_TAG
        else -> TASK_FLOW_ANALYTICS_CLICK_LOG_TAG
    }
}

/**
 * 带 Debug 点击日志的 [Modifier.clickable] 封装，仅在点击时输出，不影响重组。
 */
fun Modifier.clickWithLog(
    identifier: String,
    pageId: String? = null,
    params: Map<String, String?>? = null,
    tag: String = TASK_FLOW_ANALYTICS_CLICK_LOG_TAG,
    detail: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = clickable(
    enabled = enabled,
    onClick = {
        AnalyticsRegistry.current().trackInteraction(
            action = "click",
            operationId = identifier,
            pageId = pageId,
            params = params,
            detail = detail,
            logTag = tag,
        )
        onClick()
    },
)

/**
 * 带 Debug 长按日志的点击封装（短按不消费，仅长按触发 [onLongClick]）。
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.longClickWithLog(
    identifier: String,
    pageId: String? = null,
    params: Map<String, String?>? = null,
    tag: String = UI_LONG_CLICK_LOG_TAG,
    detail: String? = null,
    enabled: Boolean = true,
    onLongClick: () -> Unit,
): Modifier = combinedClickable(
    enabled = enabled,
    onClick = {},
    onLongClick = {
        AnalyticsRegistry.current().trackInteraction(
            action = "longClick",
            operationId = identifier,
            pageId = pageId,
            params = params,
            detail = detail,
            logTag = tag,
        )
        onLongClick()
    },
)

/**
 * 列表项点击日志：自动将 [index] 写入 [params]，与资讯 / 任务列表统一。
 *
 * @param identifier 日志 **actionId**，建议 `{page}_list_item`。
 */
fun Modifier.listItemClickWithLog(
    identifier: String,
    index: Int,
    pageId: String,
    tag: String = TASK_FLOW_ANALYTICS_LIST_ITEM_LOG_TAG,
    params: Map<String, String?>? = null,
    detail: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val mergedParams = buildMap {
        put("index", index.toString())
        params?.forEach { (key, value) ->
            if (value != null) {
                put(key, value)
            }
        }
    }
    return clickWithLog(
        identifier = identifier,
        pageId = pageId,
        tag = tag,
        params = mergedParams,
        detail = detail,
        enabled = enabled,
        onClick = onClick,
    )
}
