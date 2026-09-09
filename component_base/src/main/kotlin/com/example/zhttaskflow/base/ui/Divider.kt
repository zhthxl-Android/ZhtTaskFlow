package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider as M3Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 分隔线样式：列表内嵌分隔与区块级全宽分隔。
 */
enum class DividerStyle {
    /** 列表项之间：左右 [UiConstants.PageHorizontalPadding] 内缩。 */
    List,

    /** 区块之间：全宽，上下留白更大。 */
    Section,
}

/**
 * 全项目页面背景色唯一来源（随 Material3 浅色/深色 [MaterialTheme.colorScheme.background]）。
 */
object PageBackground {
    @Composable
    fun color(): Color = MaterialTheme.colorScheme.background
}

/**
 * 通用分隔线：统一颜色、粗细与边距；业务禁止自建 Material 分隔线样式。
 *
 * @param style [DividerStyle.List] 或 [DividerStyle.Section]
 */
@Composable
fun Divider(
    modifier: Modifier = Modifier,
    style: DividerStyle = DividerStyle.List,
) {
    val dividerColor = dividerColor()
    when (style) {
        DividerStyle.List -> {
            M3Divider(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = UiConstants.PageHorizontalPadding),
                thickness = UiConstants.DividerListThickness,
                color = dividerColor,
            )
        }

        DividerStyle.Section -> {
            M3Divider(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = UiConstants.DividerSectionVerticalPadding),
                thickness = UiConstants.DividerSectionThickness,
                color = dividerColor,
            )
        }
    }
}

@Composable
private fun dividerColor(): Color =
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = UiConstants.DividerColorAlpha)
