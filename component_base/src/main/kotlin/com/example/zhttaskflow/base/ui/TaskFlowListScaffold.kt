package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.zhttaskflow.base.ext.TaskFlowTabRootBackHandler
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * 一级 Tab 根页面脚手架：可选顶栏、FAB、滑动折叠顶栏与沉浸式头部。
 *
 * ## 沉浸式顶栏 [immersiveTop]
 * 开启后内容区不再预留状态栏 padding，Banner/轮播可延伸至状态栏下；与 [title]/[actions] 互斥（有顶栏时自动关闭沉浸）。
 * 头部文案/按钮请使用 [rememberTaskFlowStatusBarTopInset] 避开状态栏。
 *
 * @param immersiveStatusBarUseDarkIcons 状态栏图标是否深色；`null` 时随系统主题自动适配
 * @param interceptTabRootBackToDesktop 一级 Tab 根页为 `true` 时，系统返回退到桌面而非退出应用
 * @param onTabRootBackPress 自定义 Tab 根返回；默认 [com.example.zhttaskflow.base.ext.moveTaskToDesktop]
 */
@Composable
fun TaskFlowListScaffold(
    title: String? = null,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    collapsibleTopBarOnScroll: Boolean = false,
    immersiveTop: Boolean = false,
    immersiveStatusBarUseDarkIcons: Boolean? = null,
    interceptTabRootBackToDesktop: Boolean = false,
    onTabRootBackPress: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val topBarShouldCompose = taskFlowTopBarShouldCompose(
        title = title,
        hasTrailingSlot = actions != null,
    )
    val immersiveActive = immersiveTop && !topBarShouldCompose
    val collapseEnabled = collapsibleTopBarOnScroll
    val collapseIncludesStatusBar = collapseEnabled && !immersiveActive
    val topBarContentHeight = if (topBarShouldCompose) {
        TaskFlowUiConstants.PageTitleBarHeight
    } else {
        0.dp
    }
    val collapseState = rememberCollapsibleTopBarState(
        enabled = collapseEnabled,
        topBarContentHeight = topBarContentHeight,
        includeStatusBarInset = collapseIncludesStatusBar,
    )
    val density = LocalDensity.current

    TaskFlowImmersiveStatusBarEffect(
        enabled = immersiveActive,
        useDarkStatusBarIcons = immersiveStatusBarUseDarkIcons,
    )

    TaskFlowTabRootBackHandler(
        enabled = interceptTabRootBackToDesktop,
        onBack = onTabRootBackPress,
    )

    TaskFlowBaseScaffold(
        modifier = modifier,
        consumeStatusBarsInContent = !topBarShouldCompose && !collapseEnabled && !immersiveActive,
        floatingActionButton = floatingActionButton,
        contentModifier = if (collapseEnabled) {
            Modifier.nestedScroll(collapseState.nestedScrollConnection)
        } else {
            Modifier
        },
        header = {
            if (collapseEnabled) {
                val visibleHeightPx = collapseState.visibleHeaderHeightPx()
                val visibleHeight = with(density) { visibleHeightPx.toDp() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(visibleHeight)
                        .clipToBounds(),
                ) {
                    Column(
                        modifier = Modifier.offset {
                            IntOffset(0, collapseState.headerOffsetPx())
                        },
                    ) {
                        if (topBarShouldCompose) {
                            TaskFlowTopBar(
                                title = title,
                                trailing = actions,
                                applyStatusBarsPadding = true,
                                collapseProgress = 1f - collapseState.collapseProgress,
                            )
                        } else if (collapseIncludesStatusBar) {
                            Spacer(modifier = Modifier.statusBarsPadding())
                        }
                    }
                }
            } else if (topBarShouldCompose) {
                TaskFlowTopBar(
                    title = title,
                    trailing = actions,
                    applyStatusBarsPadding = true,
                )
            }
        },
        content = content,
    )
}

