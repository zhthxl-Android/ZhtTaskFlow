@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** 全局 Edge-to-Edge inset 策略（唯一入口）：Scaffold 内容 WindowInsets 为零，由子组件按场景消费 statusBars / IME。 */
object TaskFlowInsetsPolicy {
    val scaffoldContentWindowInsets: WindowInsets = WindowInsets(0, 0, 0, 0)
}

/**
 * 键盘（IME）弹出时的布局避让模式。
 *
 * - [BottomPadding]：为内容区增加与 IME 等高的底部 padding，适合整页/列表底部留白。
 * - [BringIntoView]：为可滚动容器启用 `imeNestedScroll`，并对获焦输入框 [bringIntoView]，
 *   适合弹窗表单、短内容区。
 */
enum class TaskFlowImeAvoidanceMode {
    BottomPadding,
    BringIntoView,
}

/**
 * [rememberTaskFlowImePadding] 的聚合结果：padding + 容器 Modifier，业务一次解构即可套用。
 */
@Immutable
data class TaskFlowImePaddingState(
    val padding: PaddingValues,
    val contentModifier: Modifier,
)

@Composable
fun rememberTaskFlowScaffoldContentPadding(
    scaffoldPadding: PaddingValues,
): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        start = scaffoldPadding.calculateStartPadding(layoutDirection),
        end = scaffoldPadding.calculateEndPadding(layoutDirection),
        top = 0.dp,
        bottom = scaffoldPadding.calculateBottomPadding(),
    )
}

@Composable
fun rememberTaskFlowStateBoxContentPadding(): PaddingValues = PaddingValues(0.dp)

/**
 * 沉浸式头部内避开状态栏的 top inset（Banner 图可全屏铺到顶，文字/按钮用此 padding）。
 */
@Composable
fun rememberTaskFlowStatusBarTopInset(): Dp {
    val density = LocalDensity.current
    return with(density) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
}

@Composable
fun rememberTaskFlowListLazyContentPadding(
    scaffoldPadding: PaddingValues,
    extraBottom: Dp = 0.dp,
): PaddingValues {
    val bottomInset = scaffoldPadding.calculateBottomPadding() + extraBottom
    return PaddingValues(
        start = TaskFlowUiConstants.PageHorizontalPadding,
        end = TaskFlowUiConstants.PageHorizontalPadding,
        top = 0.dp,
        bottom = bottomInset,
    )
}

/**
 * 统一 IME 键盘避让：与 [TaskFlowInsetsPolicy]（Scaffold 不消费 IME）配合，由业务内容区按需调用。
 *
 * ```
 * val ime = rememberTaskFlowImePadding(mode = TaskFlowImeAvoidanceMode.BringIntoView)
 * Column(Modifier.then(ime.contentModifier).padding(ime.padding)) {
 *     OutlinedTextField(modifier = Modifier.taskFlowImeBringIntoViewOnFocus())
 * }
 * ```
 *
 * @param mode 底部 padding 或输入框滚入可视区
 * @param extraBottom [BottomPadding] 模式下叠加的额外底部间距
 */
@Composable
fun rememberTaskFlowImePadding(
    mode: TaskFlowImeAvoidanceMode = TaskFlowImeAvoidanceMode.BottomPadding,
    extraBottom: Dp = 0.dp,
): TaskFlowImePaddingState {
    val imeBottom = rememberTaskFlowImeBottomInsetDp()
    val scrollState = rememberScrollState()
    return when (mode) {
        TaskFlowImeAvoidanceMode.BottomPadding -> {
            remember(imeBottom, extraBottom) {
                TaskFlowImePaddingState(
                    padding = PaddingValues(bottom = imeBottom + extraBottom),
                    contentModifier = Modifier,
                )
            }
        }
        TaskFlowImeAvoidanceMode.BringIntoView -> {
            remember(scrollState) {
                TaskFlowImePaddingState(
                    padding = PaddingValues(0.dp),
                    contentModifier = Modifier
                        .verticalScroll(scrollState)
                        .imeNestedScroll(),
                )
            }
        }
    }
}

/**
 * 当前 IME 底部 inset（dp）；键盘未弹出时为 0。
 */
@Composable
fun rememberTaskFlowImeBottomInsetDp(): Dp {
    val density = LocalDensity.current
    return with(density) {
        WindowInsets.ime.getBottom(this).toDp()
    }
}

/**
 * 将 [rememberTaskFlowImePadding] 的结果应用到 Modifier（先 padding 再容器滚动等行为）。
 */
fun Modifier.taskFlowImePadding(state: TaskFlowImePaddingState): Modifier {
    return this
        .padding(state.padding)
        .then(state.contentModifier)
}

/**
 * 输入框获焦时滚入可视区域；需配合 [TaskFlowImeAvoidanceMode.BringIntoView] 的父容器使用。
 */
fun Modifier.taskFlowImeBringIntoViewOnFocus(): Modifier = composed {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Modifier
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusEvent { focusState ->
            if (focusState.isFocused) {
                scope.launch {
                    bringIntoViewRequester.bringIntoView()
                }
            }
        }
}
