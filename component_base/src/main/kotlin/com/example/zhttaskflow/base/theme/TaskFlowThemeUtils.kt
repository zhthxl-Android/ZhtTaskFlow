package com.example.zhttaskflow.base.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * 应用主题模式：跟随系统、强制浅色、强制深色。
 */
enum class TaskFlowThemeMode {
    /** 跟随系统浅色/深色。 */
    FollowSystem,

    /** 始终浅色。 */
    Light,

    /** 始终深色。 */
    Dark,
}

/**
 * 全局主题模式控制器；通过 [updateThemeMode] 切换后，[TaskFlowTheme] 子树会整体重组并同步配色。
 */
@Stable
class TaskFlowThemeController(
    initialMode: TaskFlowThemeMode = TaskFlowThemeMode.FollowSystem,
) {
    var themeMode by mutableStateOf(initialMode)
        internal set

    /** 设置应用主题模式（设置页、调试入口等调用）。 */
    fun updateThemeMode(mode: TaskFlowThemeMode) {
        themeMode = mode
    }
}

/** 当前 [TaskFlowThemeController]；由 [com.example.zhttaskflow.nav.theme.TaskFlowTheme] 注入。 */
val LocalTaskFlowThemeController = compositionLocalOf<TaskFlowThemeController?> { null }

@Composable
fun rememberTaskFlowThemeController(
    initialMode: TaskFlowThemeMode = TaskFlowThemeMode.FollowSystem,
): TaskFlowThemeController = remember { TaskFlowThemeController(initialMode) }

/**
 * 是否为系统当前深色模式（不受应用强制浅色/深色影响）。
 */
@Composable
fun isSystemDarkTheme(): Boolean = isSystemInDarkTheme()

/**
 * 当前应用实际使用的深色模式（综合 [TaskFlowThemeMode] 与系统设置）。
 */
@Composable
fun isAppDarkTheme(): Boolean {
    val mode = LocalTaskFlowThemeController.current?.themeMode ?: TaskFlowThemeMode.FollowSystem
    return when (mode) {
        TaskFlowThemeMode.FollowSystem -> isSystemInDarkTheme()
        TaskFlowThemeMode.Light -> false
        TaskFlowThemeMode.Dark -> true
    }
}

/**
 * 在 [TaskFlowTheme] 外层注入 [TaskFlowThemeController]（一般由壳工程 [TaskFlowTheme] 完成，业务无需调用）。
 */
@Composable
fun TaskFlowThemeControllerProvider(
    controller: TaskFlowThemeController,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalTaskFlowThemeController provides controller) {
        content()
    }
}

/**
 * 业务层常用主题色快捷访问，避免重复书写 [MaterialTheme.colorScheme]。
 */
object TaskFlowColors {
    val colorScheme: ColorScheme
        @Composable
        get() = MaterialTheme.colorScheme

    val primary: Color
        @Composable
        get() = MaterialTheme.colorScheme.primary

    val onPrimary: Color
        @Composable
        get() = MaterialTheme.colorScheme.onPrimary

    val primaryContainer: Color
        @Composable
        get() = MaterialTheme.colorScheme.primaryContainer

    val background: Color
        @Composable
        get() = MaterialTheme.colorScheme.background

    val surface: Color
        @Composable
        get() = MaterialTheme.colorScheme.surface

    val surfaceContainerLow: Color
        @Composable
        get() = MaterialTheme.colorScheme.surfaceContainerLow

    val surfaceContainerHighest: Color
        @Composable
        get() = MaterialTheme.colorScheme.surfaceContainerHighest

    val onSurface: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurface

    val onSurfaceVariant: Color
        @Composable
        get() = MaterialTheme.colorScheme.onSurfaceVariant

    val outlineVariant: Color
        @Composable
        get() = MaterialTheme.colorScheme.outlineVariant

    val error: Color
        @Composable
        get() = MaterialTheme.colorScheme.error
}
