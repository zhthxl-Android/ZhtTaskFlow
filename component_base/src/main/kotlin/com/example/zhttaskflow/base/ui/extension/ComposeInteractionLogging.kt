package com.example.zhttaskflow.base.ui.extension

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import com.example.zhttaskflow.core.log.TaskFlowLogger

private const val UI_CLICK_LOG_TAG = "UiClick"
private const val UI_LONG_CLICK_LOG_TAG = "UiLongClick"
private const val LIST_ITEM_CLICK_LOG_TAG = "ListItem"

private fun buildInteractionLogMessage(
    action: String,
    identifier: String,
    detail: String?,
): String {
    return if (detail.isNullOrBlank()) {
        "$action id=$identifier"
    } else {
        "$action id=$identifier $detail"
    }
}

/**
 * 带 Debug 点击日志的 [Modifier.clickable] 封装，仅在点击时输出，不影响重组。
 */
fun Modifier.clickWithLog(
    identifier: String,
    tag: String = UI_CLICK_LOG_TAG,
    detail: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    Modifier.clickable(
        enabled = enabled,
        onClick = {
            TaskFlowLogger.d(tag) {
                buildInteractionLogMessage("click", identifier, detail)
            }
            onClick()
        },
    )
}

/**
 * 带 Debug 长按日志的点击封装（短按不消费，仅长按触发 [onLongClick]）。
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.longClickWithLog(
    identifier: String,
    tag: String = UI_LONG_CLICK_LOG_TAG,
    detail: String? = null,
    enabled: Boolean = true,
    onLongClick: () -> Unit,
): Modifier = composed {
    Modifier.combinedClickable(
        enabled = enabled,
        onClick = {},
        onLongClick = {
            TaskFlowLogger.d(tag) {
                buildInteractionLogMessage("longClick", identifier, detail)
            }
            onLongClick()
        },
    )
}

/**
 * 列表项点击日志：自动携带 [index] 与 [identifier]。
 */
fun Modifier.listItemClickWithLog(
    identifier: String,
    index: Int,
    tag: String = LIST_ITEM_CLICK_LOG_TAG,
    detail: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = clickWithLog(
    identifier = identifier,
    tag = tag,
    detail = "index=$index${detail?.let { ", $it" } ?: ""}",
    enabled = enabled,
    onClick = onClick,
)
