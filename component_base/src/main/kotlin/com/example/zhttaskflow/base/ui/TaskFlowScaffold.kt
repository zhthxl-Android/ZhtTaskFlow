package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 二级页面脚手架：委托 [TaskFlowPageScaffold]，保持现有 API 兼容。
 *
 * 需要返回拦截时传入 [onNavigateUp] / [onBackIntercept]；未传入时不注册系统 [BackHandler]（与原有页面行为一致）。
 */
@Composable
fun TaskFlowScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Unit)? = null,
    onBackIntercept: (() -> Boolean)? = null,
    enableSystemBackHandler: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    TaskFlowPageScaffold(
        title = title,
        modifier = modifier,
        onNavigateUp = onNavigateUp,
        onBackIntercept = onBackIntercept,
        enableSystemBackHandler = enableSystemBackHandler,
        navigationIcon = navigationIcon,
        actions = actions,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        content = content,
    )
}
