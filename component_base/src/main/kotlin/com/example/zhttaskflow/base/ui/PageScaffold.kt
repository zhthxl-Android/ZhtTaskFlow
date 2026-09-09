package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.zhttaskflow.base.ext.PageBackHandler

/**
 * 二级页面脚手架：居中标题顶栏 + 可选系统返回拦截。
 *
 * @param onNavigateUp 未拦截时的默认回退（如 [com.example.zhttaskflow.nav.AppNavigator.navigateUp]）
 * @param onBackIntercept 返回 `true` 表示已消费（如弹出未保存确认），不再执行 [onNavigateUp]
 * @param enableSystemBackHandler 为 `true` 且 [onNavigateUp] 非 null 时注册 [BackHandler]
 */
@Composable
fun PageScaffold(
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
    val shouldHandleBack = enableSystemBackHandler && onNavigateUp != null
    if (shouldHandleBack) {
        PageBackHandler(
            onNavigateUp = onNavigateUp,
            onBackIntercept = onBackIntercept,
        )
    }
    BaseScaffold(
        modifier = modifier,
        consumeStatusBarsInContent = false,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        header = {
            TopBar(
                title = title,
                leading = { navigationIcon() },
                trailing = actions,
                titlePosition = TopBarTitlePosition.Center,
                applyStatusBarsPadding = true,
            )
        },
        content = content,
    )
}
