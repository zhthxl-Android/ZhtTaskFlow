package com.example.zhttaskflow.base.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 全项目 UI 布局尺寸唯一入口（顶栏高度、页面边距、列表间距、FAB 避让等）。
 *
 * 业务与基础组件禁止硬编码相同语义的 `dp` 值，应引用本 object。
 */
object TaskFlowUiConstants {
    /** 顶栏内容区高度（一级 [TaskFlowListScaffold]、二级 [TaskFlowScaffold] 共用）。 */
    val PageTitleBarHeight = 48.dp

    /** 页面/列表水平内边距。 */
    val PageHorizontalPadding = 16.dp

    /** 顶栏插槽间距、列表项垂直间距等。 */
    val ListVerticalSpacing = 8.dp

    /** 任务列表 FAB 下方额外滚动留白（叠加 Scaffold 底距）。 */
    val FabContentExtraBottom = 72.dp

    /** 全局 Snackbar 圆角。 */
    val SnackbarCornerRadius = 12.dp

    /** 全局阻塞加载弹窗圆角。 */
    val LoadingDialogCornerRadius = 16.dp

    /** 阻塞加载全屏遮罩不透明度（0~1）。 */
    const val LoadingScrimAlpha: Float = 0.45f

    /** 确认弹窗圆角。 */
    val DialogCornerRadius = 20.dp

    /** 底部弹窗顶部圆角。 */
    val BottomSheetTopCornerRadius = 20.dp

    /** 弹窗主操作按钮最小高度。 */
    val DialogActionHeight = 48.dp

    /** 底部弹窗顶部拖拽条宽度。 */
    val BottomSheetDragHandleWidth = 32.dp

    /** 列表分隔线粗细。 */
    val DividerListThickness: Dp = 1.dp

    /** 区块分隔线粗细。 */
    val DividerSectionThickness: Dp = 1.dp

    /** 区块分隔线上下留白。 */
    val DividerSectionVerticalPadding: Dp = 12.dp

    /** 分隔线相对 [androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant] 的不透明度。 */
    const val DividerColorAlpha: Float = 0.55f

    /** 列表距底部触发「加载更多」的预取项数（距列表末尾）。 */
    const val ListLoadMorePrefetchThreshold: Int = 2

    /** 列表加载更多尾部区域最小高度。 */
    val ListLoadMoreFooterMinHeight = 48.dp
}

