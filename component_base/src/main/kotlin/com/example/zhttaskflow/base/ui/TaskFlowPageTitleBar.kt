package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.DeprecationLevel

/**
 * 历史一级 Tab 标题栏封装。
 *
 * ## 迁移说明
 * - **一级列表/Tab 根页**：使用 [TaskFlowListScaffold] 的 `title` / `actions`，无需单独顶栏组件。
 * - **自定义顶栏（三插槽）**：使用 [TaskFlowTopBar]（`leading` / `center` / `trailing`）。
 * - **二级详情页**：使用 [TaskFlowScaffold] + 居中 [TaskFlowTopBar]。
 *
 * 尺寸与边距统一见 [TaskFlowUiConstants]；系统栏 inset 见 [TaskFlowInsetsPolicy] / [TaskFlowBaseScaffold]。
 *
 * @deprecated 请迁移至 [TaskFlowTopBar] 或 [TaskFlowListScaffold]，本类型仅保留兼容旧引用。
 */
@Deprecated(
    message = "请迁移至 TaskFlowTopBar 或 TaskFlowListScaffold，见 TaskFlowPageTitleBar KDoc",
    replaceWith = ReplaceWith(
        expression = "TaskFlowTopBar(title = title, trailing = actions, modifier = modifier)",
        imports = ["com.example.zhttaskflow.base.ui.TaskFlowTopBar"],
    ),
    level = DeprecationLevel.WARNING,
)
@Composable
fun TaskFlowPageTitleBar(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TaskFlowTopBar(
        title = title,
        modifier = modifier,
        trailing = actions,
    )
}
