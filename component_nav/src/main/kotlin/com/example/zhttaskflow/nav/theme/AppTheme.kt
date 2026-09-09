package com.example.zhttaskflow.nav.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.zhttaskflow.base.theme.AppRippleTheme
import com.example.zhttaskflow.base.theme.ThemeController
import com.example.zhttaskflow.base.theme.ThemeControllerProvider
import com.example.zhttaskflow.base.theme.isAppDarkTheme
import com.example.zhttaskflow.base.theme.rememberThemeController

private const val THEME_DEPRECATION_MESSAGE = "将在下个版本移除，请使用 AppTheme（见 ReplaceWith）"

/**
 * 应用全局 Material3 主题：支持跟随系统 / 强制浅色 / 强制深色，保证全页面配色同步切换。
 *
 * @param themeController 通过 [ThemeController.updateThemeMode] 切换模式；默认跟随系统
 */
@Composable
fun AppTheme(
    themeController: ThemeController = rememberThemeController(),
    content: @Composable () -> Unit,
) {
    ThemeControllerProvider(controller = themeController) {
        val darkTheme = isAppDarkTheme()
        val colorScheme = if (darkTheme) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }
        MaterialTheme(
            colorScheme = colorScheme,
            content = {
                AppRippleTheme(content = content)
            },
        )
    }
}

@Deprecated(THEME_DEPRECATION_MESSAGE, ReplaceWith("AppTheme"))
@Composable
fun TaskFlowTheme(
    themeController: ThemeController = rememberThemeController(),
    content: @Composable () -> Unit,
) = AppTheme(themeController = themeController, content = content)
