package com.example.zhttaskflow.base.ui.extension

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import com.example.zhttaskflow.core.log.TaskFlowLogger

private const val UI_CLICK_LOG_TAG = "UiClick"
private const val UI_LONG_CLICK_LOG_TAG = "UiLongClick"
private const val LIST_ITEM_CLICK_LOG_TAG = "ListItem"
private const val UI_OUTCOME_LOG_TAG = "UiOutcome"

/**
 * Compose 层关键交互 Debug 埋点工具（与 [PageLifecycleLog] 互补）。
 *
 * ## 关键交互必埋点规范（团队标准）
 *
 * 每个业务页面至少覆盖以下三类日志，便于联调与线上问题还原：
 *
 * 1. **页面级曝光** — 使用 [PageLifecycleLog]：`onEnter` / `onLeave` / `onArgsChange`，
 *    `pageName` 与业务 Screen 一致（如 `Home`、`TaskList`），`pageArgs` 携带列表条数、加载态等快照。
 * 2. **核心 CTA 点击** — 使用 [logUiInteraction] 或 [Modifier.clickWithLog] / [listItemClickWithLog]：
 *    `action` 为 `click`、`pullRefresh` 等；`identifier`（opId）命名 `{page}_{控件}`（如 `home_entrance_card`、`task_list_fab_add`）；
 *    必须传入 [pageId]（与 [PageLifecycleLog] 的 `pageName` 对齐），关键业务字段写入 [params]。
 * 3. **操作成功 / 失败** — 在展示 Snackbar、Toast 替代物或结果回调处调用 [logUiInteraction]：
 *    `action` 使用 `success` / `failure` / `info`；`opId` 如 `{page}_snackbar` 或 `{page}_{业务}_result`；
 *    [params] 携带 `message`、错误码等（勿记录敏感信息）。
 *
 * ## 统一输出格式
 *
 * 单行键值对，字段顺序固定，便于 Logcat 过滤与 grep：
 *
 * `action={action} pageId={pageId} opId={opId} params={k1=v1,k2=v2}`
 *
 * - 未传 [pageId] 时省略 `pageId=` 段（兼容历史调用，新页面禁止省略）。
 * - [params] 与旧版 [detail] 可并存，合并进 `params` 快照。
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
    val parts = buildList {
        add("action=$action")
        if (!pageId.isNullOrBlank()) {
            add("pageId=$pageId")
        }
        add("opId=$identifier")
        val snapshot = formatInteractionParamsSnapshot(params = params, detail = detail)
        if (snapshot.isNotBlank()) {
            add("params=$snapshot")
        }
    }
    return parts.joinToString(separator = " ")
}

private fun formatInteractionParamsSnapshot(
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

/**
 * 非 [Modifier] 交互（如 [androidx.compose.material3.IconButton]、下拉刷新、结果反馈）的统一 Debug 日志。
 */
fun logUiInteraction(
    action: String,
    identifier: String,
    pageId: String? = null,
    params: Map<String, String?>? = null,
    detail: String? = null,
    tag: String = defaultTagForAction(action),
) {
    TaskFlowLogger.d(tag) {
        buildInteractionLogMessage(
            action = action,
            identifier = identifier,
            pageId = pageId,
            params = params,
            detail = detail,
        )
    }
}

private fun defaultTagForAction(action: String): String {
    return when (action) {
        "success", "failure", "info" -> UI_OUTCOME_LOG_TAG
        "longClick" -> UI_LONG_CLICK_LOG_TAG
        else -> UI_CLICK_LOG_TAG
    }
}

/**
 * 带 Debug 点击日志的 [Modifier.clickable] 封装，仅在点击时输出，不影响重组。
 */
fun Modifier.clickWithLog(
    identifier: String,
    pageId: String? = null,
    params: Map<String, String?>? = null,
    tag: String = UI_CLICK_LOG_TAG,
    detail: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = clickable(
    enabled = enabled,
    onClick = {
        TaskFlowLogger.d(tag) {
            buildInteractionLogMessage(
                action = "click",
                identifier = identifier,
                pageId = pageId,
                params = params,
                detail = detail,
            )
        }
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
        TaskFlowLogger.d(tag) {
            buildInteractionLogMessage(
                action = "longClick",
                identifier = identifier,
                pageId = pageId,
                params = params,
                detail = detail,
            )
        }
        onLongClick()
    },
)

/**
 * 列表项点击日志：自动携带 [index] 与 [identifier]。
 */
fun Modifier.listItemClickWithLog(
    identifier: String,
    index: Int,
    pageId: String? = null,
    tag: String = LIST_ITEM_CLICK_LOG_TAG,
    params: Map<String, String?>? = null,
    detail: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = clickWithLog(
    identifier = identifier,
    pageId = pageId,
    tag = tag,
    params = params,
    detail = "index=$index${detail?.let { ", $it" } ?: ""}",
    enabled = enabled,
    onClick = onClick,
)
