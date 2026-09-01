package com.example.zhttaskflow.base.ui.skeleton

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.zhttaskflow.base.ui.TaskFlowUiConstants

/** 骨架屏视觉与动画令牌（颜色随 [MaterialTheme.colorScheme] 自动适配浅/深色）。 */
object TaskFlowSkeletonDefaults {
    val LineCornerRadius = 6.dp
    val CardCornerRadius = 12.dp
    val DetailBlockCornerRadius = 8.dp

    val LineHeight = 14.dp
    val TitleLineHeight = 18.dp
    val AvatarSize = 48.dp

    val ListCardHeight = 96.dp
    val DetailHeroHeight = 180.dp

    const val ShimmerAnimationDurationMs: Int = 1_200
    const val ShimmerGradientWidthFraction: Float = 0.35f
    const val DefaultListItemCount: Int = 6
}

@Immutable
private data class TaskFlowSkeletonColors(
    val base: Color,
    val highlight: Color,
)

private val LocalTaskFlowSkeletonShimmerOffset = staticCompositionLocalOf { 0f }

/**
 * 骨架屏动画宿主：子组件共享同一套渐变扫光相位，避免多实例动画不同步。
 */
@Composable
fun TaskFlowSkeletonContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "taskflow_skeleton_shimmer")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = TaskFlowSkeletonDefaults.ShimmerAnimationDurationMs,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_offset",
    )
    CompositionLocalProvider(LocalTaskFlowSkeletonShimmerOffset provides offset) {
        Box(modifier = modifier) {
            content()
        }
    }
}

@Composable
private fun taskFlowSkeletonColors(): TaskFlowSkeletonColors {
    val scheme = MaterialTheme.colorScheme
    return TaskFlowSkeletonColors(
        base = scheme.surfaceContainerHighest,
        highlight = scheme.surfaceContainerLow,
    )
}

@Composable
private fun Modifier.taskFlowSkeletonBackground(
    shape: Shape,
): Modifier {
    val colors = taskFlowSkeletonColors()
    val phase = LocalTaskFlowSkeletonShimmerOffset.current
    val brush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to colors.base,
            0.5f to colors.highlight,
            1f to colors.base,
        ),
        start = Offset(x = phase * 800f, y = 0f),
        end = Offset(x = phase * 800f + 400f, y = 0f),
    )
    return clip(shape).background(brush = brush)
}

/** 单行文本骨架。 */
@Composable
fun TaskFlowSkeletonLine(
    modifier: Modifier = Modifier,
    width: Dp = Dp.Unspecified,
    height: Dp = TaskFlowSkeletonDefaults.LineHeight,
    cornerRadius: Dp = TaskFlowSkeletonDefaults.LineCornerRadius,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val lineModifier = if (width != Dp.Unspecified) {
        modifier
            .width(width)
            .height(height)
    } else {
        modifier
            .fillMaxWidth()
            .height(height)
    }
    Box(modifier = lineModifier.taskFlowSkeletonBackground(shape = shape))
}

/** 多行文本骨架。 */
@Composable
fun TaskFlowSkeletonMultiline(
    lineCount: Int,
    modifier: Modifier = Modifier,
    lineHeight: Dp = TaskFlowSkeletonDefaults.LineHeight,
    lastLineWidthFraction: Float = 0.65f,
    verticalSpacing: Dp = TaskFlowUiConstants.ListVerticalSpacing,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        repeat(lineCount) { index ->
            val isLast = index == lineCount - 1
            TaskFlowSkeletonLine(
                modifier = if (isLast) {
                    Modifier.fillMaxWidth(lastLineWidthFraction)
                } else {
                    Modifier.fillMaxWidth()
                },
                height = lineHeight,
            )
        }
    }
}

/** 圆形头像骨架。 */
@Composable
fun TaskFlowSkeletonCircle(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .taskFlowSkeletonBackground(shape = CircleShape),
    )
}

/** 矩形块骨架（卡片底图、封面等）。 */
@Composable
fun TaskFlowSkeletonRect(
    modifier: Modifier = Modifier,
    height: Dp,
    cornerRadius: Dp = TaskFlowSkeletonDefaults.CardCornerRadius,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .taskFlowSkeletonBackground(shape = RoundedCornerShape(cornerRadius)),
    )
}

/** 列表卡片行骨架：左侧头像 + 右侧多行文本。 */
@Composable
fun TaskFlowSkeletonCardRow(
    modifier: Modifier = Modifier,
    avatarSize: Dp = TaskFlowSkeletonDefaults.AvatarSize,
    textLines: Int = 3,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.PageHorizontalPadding),
    ) {
        TaskFlowSkeletonCircle(size = avatarSize)
        TaskFlowSkeletonMultiline(
            lineCount = textLines,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 预置场景模板。 */
enum class TaskFlowSkeletonTemplate {
    /** 纯矩形卡片列表（资讯/任务列表等）。 */
    List,

    /** 带头像的卡片列表。 */
    Card,

    /** 详情页：顶图 + 标题 + 正文。 */
    Detail,
}

/** 列表页默认卡片骨架项。 */
@Composable
fun TaskFlowSkeletonListCardItem(
    modifier: Modifier = Modifier,
    height: Dp = TaskFlowSkeletonDefaults.ListCardHeight,
) {
    TaskFlowSkeletonRect(
        modifier = modifier,
        height = height,
        cornerRadius = TaskFlowSkeletonDefaults.CardCornerRadius,
    )
}

/** 列表骨架模板（[LazyColumn]）。 */
@Composable
fun TaskFlowSkeletonListTemplate(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    itemCount: Int = TaskFlowSkeletonDefaults.DefaultListItemCount,
    template: TaskFlowSkeletonTemplate = TaskFlowSkeletonTemplate.List,
) {
    TaskFlowSkeletonContainer(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.ListVerticalSpacing),
        ) {
            items(itemCount) {
                when (template) {
                    TaskFlowSkeletonTemplate.List -> TaskFlowSkeletonListCardItem()
                    TaskFlowSkeletonTemplate.Card -> TaskFlowSkeletonCardRow()
                    TaskFlowSkeletonTemplate.Detail -> TaskFlowSkeletonListCardItem()
                }
            }
        }
    }
}

/** 详情页骨架：封面 + 标题行 + 多行正文。 */
@Composable
fun TaskFlowSkeletonDetailTemplate(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.PageHorizontalPadding),
    ) {
        TaskFlowSkeletonRect(
            height = TaskFlowSkeletonDefaults.DetailHeroHeight,
            cornerRadius = TaskFlowSkeletonDefaults.DetailBlockCornerRadius,
        )
        TaskFlowSkeletonLine(
            height = TaskFlowSkeletonDefaults.TitleLineHeight,
            width = 240.dp,
        )
        TaskFlowSkeletonMultiline(lineCount = 5)
        Spacer(modifier = Modifier.height(TaskFlowUiConstants.ListVerticalSpacing))
        TaskFlowSkeletonMultiline(lineCount = 4, lastLineWidthFraction = 0.5f)
    }
}

/** 详情页全屏骨架（含扫光容器）。 */
@Composable
fun TaskFlowSkeletonDetailScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    TaskFlowSkeletonContainer(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
        ) {
            TaskFlowSkeletonDetailTemplate()
        }
    }
}
