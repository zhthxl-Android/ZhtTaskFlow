package com.example.zhttaskflow.nav.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.zhttaskflow.base.theme.TaskFlowRippleTheme

/**
 * 应用全局 Material3 主题：跟随系统浅色/深色，保证底部导航与标题栏对比度。
 */
@Composable
fun TaskFlowTheme(
    content: @Composable () -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) {
        darkColorScheme()
    } else {
        lightColorScheme()
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = {
            TaskFlowRippleTheme(content = content)
        },
    )
}
