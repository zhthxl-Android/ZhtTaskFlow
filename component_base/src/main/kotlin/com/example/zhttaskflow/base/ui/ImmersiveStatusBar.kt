package com.example.zhttaskflow.base.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.example.zhttaskflow.base.theme.isAppDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 一级页沉浸式顶栏：控制状态栏图标/文字深浅，保证与头部 Banner 背景对比度。
 *
 * @param enabled 是否处于沉浸式顶栏（内容延伸至状态栏下）
 * @param useDarkStatusBarIcons `true` 深色图标（浅色背景）；`false` 浅色图标（深色背景）；`null` 跟随系统浅色/深色主题
 */
@Composable
fun ImmersiveStatusBarEffect(
    enabled: Boolean,
    useDarkStatusBarIcons: Boolean?,
) {
    val view = LocalView.current
    val darkTheme = isAppDarkTheme()
    val themeDefaultDarkIcons = !darkTheme
    val resolvedDarkIcons = when {
        !enabled -> themeDefaultDarkIcons
        useDarkStatusBarIcons != null -> useDarkStatusBarIcons
        else -> themeDefaultDarkIcons
    }
    DisposableEffect(enabled, resolvedDarkIcons, darkTheme) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            return@DisposableEffect onDispose { }
        }
        val controller = WindowInsetsControllerCompat(window, view)
        controller.isAppearanceLightStatusBars = resolvedDarkIcons
        onDispose {
            controller.isAppearanceLightStatusBars = themeDefaultDarkIcons
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

