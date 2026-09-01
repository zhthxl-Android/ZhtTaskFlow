package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 分隔线样式：列表内嵌分隔与区块级全宽分隔。
 */
enum class TaskFlowDividerStyle {
    /** 列表项之间：左右 [TaskFlowUiConstants.PageHorizontalPadding] 内缩。 */
    List,

    /** 区块之间：全宽，上下留白更大。 */
    Section,
}

/**
 * 全项目页面背景色唯一来源（随 Material3 浅色/深色 [MaterialTheme.colorScheme.background]）。
 */
object TaskFlowPageBackground {
    @Composable
    fun color(): Color = MaterialTheme.colorScheme.background
}

/**
 * 通用分隔线：统一颜色、粗细与边距；业务禁止自建 [HorizontalDivider] 样式。
 *
 * @param style [TaskFlowDividerStyle.List] 或 [TaskFlowDividerStyle.Section]
 */
@Composable
fun TaskFlowDivider(
    modifier: Modifier = Modifier,
    style: TaskFlowDividerStyle = TaskFlowDividerStyle.List,
) {
    val dividerColor = taskFlowDividerColor()
    when (style) {
        TaskFlowDividerStyle.List -> {
            HorizontalDivider(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = TaskFlowUiConstants.PageHorizontalPadding),
                thickness = TaskFlowUiConstants.DividerListThickness,
                color = dividerColor,
            )
        }

        TaskFlowDividerStyle.Section -> {
            HorizontalDivider(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = TaskFlowUiConstants.DividerSectionVerticalPadding),
                thickness = TaskFlowUiConstants.DividerSectionThickness,
                color = dividerColor,
            )
        }
    }
}

@Composable
private fun taskFlowDividerColor(): Color =
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = TaskFlowUiConstants.DividerColorAlpha)
