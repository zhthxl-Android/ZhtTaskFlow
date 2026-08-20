package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 全项目统一页面脚手架：收敛 [Scaffold] + [TopAppBar] 样板，与 [StateBox] 等同属基础 UI 层。
 *
 * ## 用途
 * 业务列表/详情页的最外层结构容器，统一顶部栏样式与 [PaddingValues] 传递方式。
 * 顶部栏使用 [CenterAlignedTopAppBar]，标题与操作区样式遵循 Material3 主题默认值。
 *
 * ## 适用场景
 * - 标准单标题页面（首页、任务列表、资讯列表等）
 * - 需扩展右上角 [actions]、底部 [bottomBar] 或 FAB（[floatingActionButton]）的页面
 *
 * @param title 顶部栏标题文案（由调用方传入已解析的字符串，通常来自 stringResource）
 * @param modifier 根布局修饰符
 * @param navigationIcon 顶部栏左侧导航区（如返回 [IconButton]），默认无
 * @param actions 顶部栏右侧操作区，默认无
 * @param bottomBar 底部栏插槽，默认无
 * @param floatingActionButton 悬浮操作按钮插槽，默认无（如任务列表新增按钮）
 * @param content 页面主体；[PaddingValues] 由 Scaffold 提供，交由 [StateBox] 的 [contentPadding] 统一消费，本组件内部不叠加 padding
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = title)
                },
                navigationIcon = navigationIcon,
                actions = actions,
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
    ) { innerPadding ->
        content(innerPadding)
    }
}
