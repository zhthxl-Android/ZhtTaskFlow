package com.example.zhttaskflow.base.extension

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Compose 通用扩展，比如修饰符扩展、状态快速获取、密度相关工具
 * */


/**
 * 为 Modifier 应用统一页面内边距（16dp）。
 */
@Stable
fun Modifier.pagePadding(): Modifier = composed {
    padding(horizontal = 16.dp, vertical = 12.dp)
}

/**
 * 根据 [PaddingValues] 应用 padding，兼容 Scaffold contentPadding。
 */
fun Modifier.paddingValues(paddingValues: PaddingValues): Modifier = composed {
    padding(paddingValues)
}
