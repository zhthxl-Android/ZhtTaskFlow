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
import com.example.zhttaskflow.base.ui.UiConstants

@Immutable
private data class SkeletonColors(
    val base: Color,
    val highlight: Color,
)

private val LocalSkeletonShimmerOffset = staticCompositionLocalOf { 0f }

/**
 * 骨架屏动画宿主：子组件共享同一套渐变扫光相位，避免多实例动画不同步。
 */
@Composable
fun SkeletonContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "taskflow_skeleton_shimmer")
    val offset by transition.animateFloat(
        initialValue = UiConstants.SkeletonShimmerOffsetInitial,
        targetValue = UiConstants.SkeletonShimmerOffsetTarget,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = UiConstants.SkeletonShimmerAnimationDurationMs,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_offset",
    )
    CompositionLocalProvider(LocalSkeletonShimmerOffset provides offset) {
        Box(modifier = modifier) {
            content()
        }
    }
}

@Composable
private fun skeletonColors(): SkeletonColors {
    val scheme = MaterialTheme.colorScheme
    return SkeletonColors(
        base = scheme.surfaceContainerHighest,
        highlight = scheme.surfaceContainerLow,
    )
}

@Composable
private fun Modifier.skeletonBackground(
    shape: Shape,
): Modifier {
    val colors = skeletonColors()
    val phase = LocalSkeletonShimmerOffset.current
    val travel = UiConstants.SkeletonShimmerGradientTravelPx
    val band = UiConstants.SkeletonShimmerGradientBandPx
    val brush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to colors.base,
            0.5f to colors.highlight,
            1f to colors.base,
        ),
        start = Offset(x = phase * travel, y = 0f),
        end = Offset(x = phase * travel + band, y = 0f),
    )
    return clip(shape).background(brush = brush)
}

/** 单行文本骨架。 */
@Composable
fun SkeletonLine(
    modifier: Modifier = Modifier,
    width: Dp = Dp.Unspecified,
    height: Dp = UiConstants.SkeletonLineHeight,
    cornerRadius: Dp = UiConstants.SkeletonLineCornerRadius,
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
    Box(modifier = lineModifier.skeletonBackground(shape = shape))
}

/** 多行文本骨架。 */
@Composable
fun SkeletonMultiline(
    lineCount: Int,
    modifier: Modifier = Modifier,
    lineHeight: Dp = UiConstants.SkeletonLineHeight,
    lastLineWidthFraction: Float = UiConstants.SkeletonMultilineLastLineWidthFraction,
    verticalSpacing: Dp = UiConstants.ListVerticalSpacing,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        repeat(lineCount) { index ->
            val isLast = index == lineCount - 1
            SkeletonLine(
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
fun SkeletonCircle(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .skeletonBackground(shape = CircleShape),
    )
}

/** 矩形块骨架（卡片底图、封面等）。 */
@Composable
fun SkeletonRect(
    modifier: Modifier = Modifier,
    height: Dp,
    cornerRadius: Dp = UiConstants.SkeletonCardCornerRadius,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .skeletonBackground(shape = RoundedCornerShape(cornerRadius)),
    )
}

/** 列表卡片行骨架：左侧头像 + 右侧多行文本。 */
@Composable
fun SkeletonCardRow(
    modifier: Modifier = Modifier,
    avatarSize: Dp = UiConstants.SkeletonAvatarSize,
    textLines: Int = UiConstants.SkeletonCardRowTextLineCount,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UiConstants.PageHorizontalPadding),
    ) {
        SkeletonCircle(size = avatarSize)
        SkeletonMultiline(
            lineCount = textLines,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 预置场景模板。 */
enum class SkeletonTemplate {
    /** 纯矩形卡片列表（资讯/任务列表等）。 */
    List,

    /** 带头像的卡片列表。 */
    Card,

    /** 详情页：顶图 + 标题 + 正文。 */
    Detail,
}

/** 列表页默认卡片骨架项。 */
@Composable
fun SkeletonListCardItem(
    modifier: Modifier = Modifier,
    height: Dp = UiConstants.SkeletonListCardHeight,
) {
    SkeletonRect(
        modifier = modifier,
        height = height,
        cornerRadius = UiConstants.SkeletonCardCornerRadius,
    )
}

/** 列表骨架模板（[LazyColumn]）。 */
@Composable
fun SkeletonListTemplate(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    itemCount: Int = UiConstants.SkeletonDefaultListItemCount,
    template: SkeletonTemplate = SkeletonTemplate.List,
) {
    SkeletonContainer(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(UiConstants.ListVerticalSpacing),
        ) {
            items(itemCount) {
                when (template) {
                    SkeletonTemplate.List -> SkeletonListCardItem()
                    SkeletonTemplate.Card -> SkeletonCardRow()
                    SkeletonTemplate.Detail -> SkeletonListCardItem()
                }
            }
        }
    }
}
