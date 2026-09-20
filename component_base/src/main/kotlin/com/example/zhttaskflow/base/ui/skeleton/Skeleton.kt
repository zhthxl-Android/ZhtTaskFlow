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

/**
 * 骨架颜色数据类
 * */
@Immutable
private data class SkeletonColors(
    val base: Color,//底色
    val highlight: Color,//高光色
)

/**
 * 全局扫光偏移
 * 整个骨架屏 动画同步的核心
 * 存储扫光动画的当前偏移量（0f~1f），所有子骨架组件都读取这个值，保证所有位置的扫光完全同步
 * */
private val LocalSkeletonShimmerOffset = staticCompositionLocalOf { 0f }

/**
 * 骨架屏动画宿主：
 * 统一管理无限循环动画，生成扫光相位，通过 CompositionLocal 向下共享
 * 子组件共享同一套渐变扫光相位，避免多实例动画不同步。
 * 所有需要扫光动画的骨架都必须放在这个容器里，才能获得同步的动画效果
 */
@Composable
fun SkeletonContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    //无限运行的过渡动画宿主，不会自动停止
    val transition = rememberInfiniteTransition(label = "taskflow_skeleton_shimmer")
    //扫光动画的偏移量
    val offset by transition.animateFloat(
        initialValue = UiConstants.SkeletonShimmerOffsetInitial,//动画的起始值
        targetValue = UiConstants.SkeletonShimmerOffsetTarget,//动画的结束值
        //无限循环
        animationSpec = infiniteRepeatable(
            //补间动画
            animation = tween(
                durationMillis = UiConstants.SkeletonShimmerAnimationDurationMs,//动画时长
                easing = LinearEasing,//线性匀速
            ),
            repeatMode = RepeatMode.Restart,//循环
        ),
        label = "shimmer_offset",
    )
    CompositionLocalProvider(LocalSkeletonShimmerOffset provides offset) {
        Box(modifier = modifier) {
            content()
        }
    }
}

/**
 * 从当前 主题中自动获取骨架的底色和高光色
 * 自动适配浅色 / 深色主题，不用手动写两套颜色
 * */
@Composable
private fun skeletonColors(): SkeletonColors {
    val scheme = MaterialTheme.colorScheme
    return SkeletonColors(
        base = scheme.surfaceContainerHighest,
        highlight = scheme.surfaceContainerLow,
    )
}

/**
 * 绘制核心层
 * 读取全局相位，动态计算线性渐变位置，结合形状裁剪，实现单块骨架效果
 * 任意组件加上骨架渐变背景和形状裁剪
 * */
@Composable
private fun Modifier.skeletonBackground(
    shape: Shape,//骨架的形状，比如圆形、圆角矩形，决定最终骨架的外形
): Modifier {
    val colors = skeletonColors()//当前主题的骨架颜色
    val phase = LocalSkeletonShimmerOffset.current//当前骨架扫光动画的偏移量
    val travel = UiConstants.SkeletonShimmerGradientTravelPx//渐变扫过的总距离（像素）
    val band = UiConstants.SkeletonShimmerGradientBandPx//渐变高光带的宽度（像素）
    //构建动态线性渐变
    val brush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to colors.base,//起始颜色
            0.5f to colors.highlight,//中间颜色
            1f to colors.base,//结束颜色
        ),
        start = Offset(x = phase * travel, y = 0f),//渐变动画的起始位置
        end = Offset(x = phase * travel + band, y = 0f),//渐变动画的结束位置
    )
    return clip(shape).background(brush = brush)//裁剪形状并填充渐变背景
}

/** 单行文本骨架。 比如标题、单行文本 */
@Composable
fun SkeletonLine(
    modifier: Modifier = Modifier,
    width: Dp = Dp.Unspecified,//宽度；不指定则默认撑满父容器宽度
    height: Dp = UiConstants.SkeletonLineHeight,//骨架高度，默认和真实文本行高一致
    cornerRadius: Dp = UiConstants.SkeletonLineCornerRadius,//圆角大小，模拟文字的圆角感
) {
    val shape = RoundedCornerShape(cornerRadius)
    //指定宽度就用固定宽度，否则撑满宽度
    val lineModifier = if (width != Dp.Unspecified) {
        modifier
            .width(width)
            .height(height)
    } else {
        modifier
            .fillMaxWidth()
            .height(height)
    }
    //加上 `skeletonBackground` 修饰符，实现骨架效果
    Box(modifier = lineModifier.skeletonBackground(shape = shape))
}

/** 多行文本骨架。 */
@Composable
fun SkeletonMultiline(
    lineCount: Int,//行数
    modifier: Modifier = Modifier,
    lineHeight: Dp = UiConstants.SkeletonLineHeight,//单行高度
    lastLineWidthFraction: Float = UiConstants.SkeletonMultilineLastLineWidthFraction,//最后一行的宽度比例，比如 0.7，模拟真实文本最后一行没满的效果，更逼真
    verticalSpacing: Dp = UiConstants.ListVerticalSpacing,//行之间的垂直间距，和真实列表间距一致
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
    ) {
        //逐行绘制
        repeat(lineCount) { index ->
            val isLast = index == lineCount - 1
            SkeletonLine(
                modifier = if (isLast) {
                    //最后一行按比例缩短
                    Modifier.fillMaxWidth(lastLineWidthFraction)
                } else {
                    Modifier.fillMaxWidth()
                },
                height = lineHeight,
            )
        }
    }
}

/**
 *  圆形头像骨架。
 * 模拟圆形头像、圆形图标占位
 * */
@Composable
fun SkeletonCircle(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .skeletonBackground(shape = CircleShape),//加上骨架背景
    )
}

/** 矩形块骨架（卡片底图、封面等）。 */
@Composable
fun SkeletonRect(
    modifier: Modifier = Modifier,
    height: Dp,//高度
    cornerRadius: Dp = UiConstants.SkeletonCardCornerRadius,//圆角大小
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .skeletonBackground(shape = RoundedCornerShape(cornerRadius)),//圆角矩形形状，加上骨架背景
    )
}

/** 列表卡片行骨架：左侧头像 + 右侧多行文本。 */
@Composable
fun SkeletonCardRow(
    modifier: Modifier = Modifier,
    avatarSize: Dp = UiConstants.SkeletonAvatarSize,//头像大小
    textLines: Int = UiConstants.SkeletonCardRowTextLineCount,//右侧文本行数
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(UiConstants.PageHorizontalPadding),
    ) {
        SkeletonCircle(size = avatarSize)//头像骨架
        SkeletonMultiline(//多行文本骨架
            lineCount = textLines,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 *  预置场景模板。
 * 直接选模板即可，不用自己组合骨架组件
 * */
enum class SkeletonTemplate {
    /** 纯矩形卡片列表（资讯/任务列表等）。 */
    List,

    /** 带头像的卡片列表。 */
    Card,

    /** 详情页：顶图 + 标题 + 正文。 */
    Detail,
}

/**
 * 列表页----默认卡片骨架项。
 * */
@Composable
fun SkeletonListCardItem(
    modifier: Modifier = Modifier,
    height: Dp = UiConstants.SkeletonListCardHeight,
) {
    //矩形骨架
    SkeletonRect(
        modifier = modifier,
        height = height,
        cornerRadius = UiConstants.SkeletonCardCornerRadius,
    )
}

/**
 * 列表骨架模板---完整骨架屏
 * 生成一整个和真实列表布局完全一致的骨架
 * */
@Composable
fun SkeletonListTemplate(
    contentPadding: PaddingValues,//内边距，必须和真实列表的 `contentPadding` 完全一致，保证切换时不跳动
    modifier: Modifier = Modifier,
    itemCount: Int = UiConstants.SkeletonDefaultListItemCount,
    template: SkeletonTemplate = SkeletonTemplate.List,
) {
    //骨架容器
    SkeletonContainer(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(UiConstants.ListVerticalSpacing),
        ) {
            items(itemCount) {
                when (template) {
                    SkeletonTemplate.List -> SkeletonListCardItem()//矩形骨架
                    SkeletonTemplate.Card -> SkeletonCardRow()//列表卡片行骨架：左侧头像 + 右侧多行文本
                    SkeletonTemplate.Detail -> SkeletonListCardItem()//矩形骨架
                }
            }
        }
    }
}
