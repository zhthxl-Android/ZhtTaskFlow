package com.example.zhttaskflow.base.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

/**
 * 主题扩展函数，统一颜色、字体、形状的获取方式，适配深色模式等
 * */

/**
 * 页面标题文本样式扩展。
 */
@Composable
fun basePageTitleStyle(): TextStyle = MaterialTheme.typography.headlineSmall

/**
 * 辅助说明文本样式扩展。
 */
@Composable
fun baseBodySecondaryStyle(): TextStyle =
    MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
