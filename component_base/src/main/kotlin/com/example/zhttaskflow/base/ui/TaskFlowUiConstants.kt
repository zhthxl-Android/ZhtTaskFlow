package com.example.zhttaskflow.base.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 全项目 UI 布局尺寸唯一入口（顶栏高度、页面边距、列表间距、加载指示器、FAB 避让、骨架屏等）。
 *
 * 业务与基础组件禁止硬编码相同语义的 `dp` 值，应引用本 object。
 */
object TaskFlowUiConstants {
    /** 顶栏内容区高度（一级 [TaskFlowListScaffold]、二级 [TaskFlowScaffold] / [TaskFlowTopBar] 共用）。 */
    val TopBarHeight = 48.dp

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

    /** 阻塞加载卡片阴影高度。 */
    val LoadingDialogElevation = 6.dp

    /** 阻塞加载卡片内容水平内边距。 */
    val LoadingDialogContentPaddingHorizontal = 28.dp

    /** 阻塞加载卡片内容垂直内边距。 */
    val LoadingDialogContentPaddingVertical = 24.dp

    /** 阻塞加载指示器与文案间距。 */
    val LoadingDialogContentSpacing = 16.dp

    /** 阻塞加载 [androidx.compose.material3.CircularProgressIndicator] 尺寸。 */
    val LoadingDialogProgressSize = 40.dp

    /** 阻塞加载进度环描边宽度。 */
    val LoadingDialogProgressStrokeWidth = 3.dp

    /** 列表加载更多尾部 [androidx.compose.material3.CircularProgressIndicator] 高度。 */
    val ListLoadMoreProgressHeight = 28.dp

    /** 列表加载更多进度环描边宽度。 */
    val ListLoadMoreProgressStrokeWidth = 2.dp

    /** Snackbar / 加载层 / 弹窗正文等紧凑字号（与 [androidx.compose.material3.MaterialTheme.typography.bodyMedium] 对齐）。 */
    val CompactBodyTextSize: TextUnit = 14.sp

    /** Snackbar 左侧类型图标尺寸。 */
    val SnackbarLeadingIconSize = 20.dp

    /** Snackbar 图标与文案水平间距。 */
    val SnackbarLeadingIconSpacing = 12.dp

    /** 空态 / 错误态等内容区外边距。 */
    val StateScreenContentPadding = 24.dp

    /** 错误态重试按钮与文案的上间距。 */
    val StateScreenActionTopSpacing = 16.dp

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

    // region 骨架屏（[com.example.zhttaskflow.base.ui.skeleton]）

    /** 骨架单行/多行文本圆角。 */
    val SkeletonLineCornerRadius = 6.dp

    /** 骨架列表卡片圆角。 */
    val SkeletonCardCornerRadius = 12.dp

    /** 骨架详情区块（封面等）圆角。 */
    val SkeletonDetailBlockCornerRadius = 8.dp

    /** 骨架正文行高。 */
    val SkeletonLineHeight = 14.dp

    /** 骨架标题行高。 */
    val SkeletonTitleLineHeight = 18.dp

    /** 骨架详情页标题行占位宽度。 */
    val SkeletonDetailTitleLineWidth = 240.dp

    /** 骨架列表卡片行内头像尺寸。 */
    val SkeletonAvatarSize = 48.dp

    /** 骨架列表单卡占位高度。 */
    val SkeletonListCardHeight = 96.dp

    /** 骨架详情页顶图占位高度。 */
    val SkeletonDetailHeroHeight = 180.dp

    /** 扫光动画时长（毫秒）。 */
    const val SkeletonShimmerAnimationDurationMs: Int = 1_200

    /** 扫光渐变相对宽度的比例（预留扩展，当前扫光带由 [SkeletonShimmerGradientBandPx] 控制）。 */
    const val SkeletonShimmerGradientWidthFraction: Float = 0.35f

    /** 扫光相位动画起始值（归一化）。 */
    const val SkeletonShimmerOffsetInitial: Float = -1f

    /** 扫光相位动画结束值（归一化）。 */
    const val SkeletonShimmerOffsetTarget: Float = 2f

    /** 扫光渐变在绘制坐标系中的行程（px，与相位相乘）。 */
    const val SkeletonShimmerGradientTravelPx: Float = 800f

    /** 扫光高亮带宽度（px）。 */
    const val SkeletonShimmerGradientBandPx: Float = 400f

    /** 骨架列表卡片行右侧默认文本行数。 */
    const val SkeletonCardRowTextLineCount: Int = 3

    /** 列表骨架默认占位条数。 */
    const val SkeletonDefaultListItemCount: Int = 6

    /** 多行骨架末行默认宽度占比。 */
    const val SkeletonMultilineLastLineWidthFraction: Float = 0.65f

    /** 详情骨架第二段多行末行宽度占比。 */
    const val SkeletonDetailSecondaryMultilineLastLineWidthFraction: Float = 0.5f

    /** 详情骨架第一段正文行数。 */
    const val SkeletonDetailPrimaryMultilineCount: Int = 5

    /** 详情骨架第二段正文行数。 */
    const val SkeletonDetailSecondaryMultilineCount: Int = 4

    // endregion
}

