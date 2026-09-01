package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 二级页面脚手架：在 [TaskFlowBaseScaffold] 上扩展居中标题顶栏与返回区。
 *
 * 顶栏通过 [TaskFlowTopBar] 消费状态栏安全区；内容区使用核心层统一 inset 与边距。
 */
@Composable
fun TaskFlowScaffold(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    TaskFlowBaseScaffold(
        modifier = modifier,
        consumeStatusBarsInContent = false,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        header = {
            TaskFlowTopBar(
                title = title,
                leading = { navigationIcon() },
                trailing = actions,
                titlePosition = TaskFlowTopBarTitlePosition.Center,
                applyStatusBarsPadding = true,
            )
        },
        content = content,
    )
}
