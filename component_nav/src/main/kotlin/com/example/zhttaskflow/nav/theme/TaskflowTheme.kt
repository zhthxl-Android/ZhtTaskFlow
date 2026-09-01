package com.example.zhttaskflow.nav.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.zhttaskflow.base.theme.TaskFlowRippleTheme
import com.example.zhttaskflow.base.theme.TaskFlowThemeController
import com.example.zhttaskflow.base.theme.TaskFlowThemeControllerProvider
import com.example.zhttaskflow.base.theme.isAppDarkTheme
import com.example.zhttaskflow.base.theme.rememberTaskFlowThemeController

/**
 * 应用全局 Material3 主题：支持跟随系统 / 强制浅色 / 强制深色，保证全页面配色同步切换。
 *
 * @param themeController 通过 [TaskFlowThemeController.updateThemeMode] 切换模式；默认跟随系统
 */
@Composable
fun TaskFlowTheme(
    themeController: TaskFlowThemeController = rememberTaskFlowThemeController(),
    content: @Composable () -> Unit,
) {
    TaskFlowThemeControllerProvider(controller = themeController) {
        val darkTheme = isAppDarkTheme()
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
}
