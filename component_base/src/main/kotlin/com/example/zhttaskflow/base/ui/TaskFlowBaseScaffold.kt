package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * 核心页面脚手架：统一 [TaskFlowInsetsPolicy]、系统栏与内容区边距，不含顶栏/导航等业务层级 UI。
 */
@Composable
fun TaskFlowBaseScaffold(
    modifier: Modifier = Modifier,
    consumeStatusBarsInContent: Boolean,
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    header: @Composable () -> Unit = {},
    contentModifier: Modifier = Modifier,
    content: @Composable (scaffoldContentPadding: PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = TaskFlowInsetsPolicy.scaffoldContentWindowInsets,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
    ) { innerPadding ->
        val contentInsets = rememberTaskFlowScaffoldContentPadding(scaffoldPadding = innerPadding)
        Column(modifier = Modifier.fillMaxSize()) {
            header()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(
                        if (consumeStatusBarsInContent) {
                            Modifier.statusBarsPadding()
                        } else {
                            Modifier
                        },
                    )
                    .then(contentModifier)
                    .padding(contentInsets),
            ) {
                content(contentInsets)
            }
        }
    }
}
