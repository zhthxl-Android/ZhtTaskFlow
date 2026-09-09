package com.example.zhttaskflow.base.theme

// 与 material `RippleTheme` 的 import 别名约定（本模块将 deprecated 引用视为编译错误，故用 FQN 说明冲突来源）：
// import androidx.compose.material.ripple.RippleTheme as MaterialRippleTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 全局水波纹视觉令牌：颜色、透明度、半径在浅色/深色下统一由此定义。
 *
 * 应用层入口为 [AppRippleTheme]；与 `androidx.compose.material.ripple.RippleTheme`（Material 旧版
 * [LocalRippleTheme] API，已废弃）区分，避免若将本 Composable 命名为 `RippleTheme` 时的 import 冲突。
 */
object RippleTokens {
    /** 有界水波纹最大半径（Material3 组件与 [androidx.compose.foundation.clickable] 共用）。 */
    val BoundedRippleRadius: Dp = 28.dp

    /** 水波纹基色（浅色/深色下对 [ColorScheme.primary] 做轻微调校）。 */
    fun rippleColor(colorScheme: ColorScheme, darkTheme: Boolean) =
        if (darkTheme) {
            colorScheme.primary.copy(alpha = 0.85f)
        } else {
            colorScheme.primary
        }

    fun rippleAlpha(darkTheme: Boolean): RippleAlpha =
        if (darkTheme) {
            RippleAlpha(
                pressedAlpha = 0.14f,
                focusedAlpha = 0.12f,
                draggedAlpha = 0.18f,
                hoveredAlpha = 0.08f,
            )
        } else {
            RippleAlpha(
                pressedAlpha = 0.10f,
                focusedAlpha = 0.10f,
                draggedAlpha = 0.14f,
                hoveredAlpha = 0.06f,
            )
        }
}

private const val RIPPLE_THEME_DEPRECATION = "将在下个版本移除，请使用 AppRippleTheme（见 ReplaceWith）"

/**
 * 在 [MaterialTheme] 之下注入全局 [LocalRippleConfiguration]，
 * Material3 可点击组件与遵循 M3 Ripple 的 [androidx.compose.foundation.clickable] 自动统一水波纹。
 *
 * 由 [com.example.zhttaskflow.nav.theme.AppTheme] 自动包裹，业务侧无需单独配置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRippleTheme(
    content: @Composable () -> Unit,
) {
    val darkTheme = isAppDarkTheme()
    val colorScheme = MaterialTheme.colorScheme
    val rippleAlpha = remember(darkTheme) { RippleTokens.rippleAlpha(darkTheme) }
    val rippleColor = remember(colorScheme, darkTheme) {
        RippleTokens.rippleColor(colorScheme, darkTheme)
    }
    val rippleConfiguration = remember(rippleColor, rippleAlpha) {
        RippleConfiguration(
            color = rippleColor,
            rippleAlpha = rippleAlpha,
        )
    }
    CompositionLocalProvider(
        LocalRippleConfiguration provides rippleConfiguration,
    ) {
        content()
    }
}

@Deprecated(RIPPLE_THEME_DEPRECATION, ReplaceWith("AppRippleTheme"))
@Composable
fun TaskFlowRippleTheme(
    content: @Composable () -> Unit,
) = AppRippleTheme(content = content)

/**
 * 无波纹点击变体：用于全屏遮罩、自定义按压反馈等不需要水波纹的场景。
 */
@Composable
fun Modifier.taskFlowClickableNoRipple(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return clickable(
        interactionSource = interactionSource,
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )
}

/**
 * 禁用全局水波纹的局部作用域（Material3 可点击组件不绘制水波纹）。
 *
 * 子树内若使用 [androidx.compose.foundation.clickable] 且未指定 `indication`，请改用 [taskFlowClickableNoRipple]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoRippleContent(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides null,
    ) {
        content()
    }
}
