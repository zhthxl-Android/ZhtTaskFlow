package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost as M3SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals as M3SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.example.zhttaskflow.base.R
import com.example.zhttaskflow.base.ui.icon.AppIcons

/**
 * 全局 Snackbar 预设类型：成功 / 错误 / 普通，对应配色、图标与默认展示时长。
 */
enum class SnackbarType {
    Success,
    Error,
    Normal,
}

/**
 * 带类型的 [M3SnackbarVisuals]，供 [SnackbarHost] 渲染统一样式。
 */
@Immutable
data class SnackbarVisuals(
    override val message: String,
    val type: SnackbarType,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = type.defaultDuration,
) : M3SnackbarVisuals

val SnackbarType.defaultDuration: SnackbarDuration
    get() = when (this) {
        SnackbarType.Success -> SnackbarDuration.Short
        SnackbarType.Error -> SnackbarDuration.Long
        SnackbarType.Normal -> SnackbarDuration.Short
    }

@Immutable
private data class SnackbarStyle(
    val containerColor: Color,
    val contentColor: Color,
    val icon: ImageVector,
)

private fun snackbarStyle(type: SnackbarType): SnackbarStyle {
    return when (type) {
        SnackbarType.Success -> SnackbarStyle(
            containerColor = Color(0xFF2E7D32),
            contentColor = Color(0xFFFFFFFF),
            icon = AppIcons.Snackbar.Success,
        )
        SnackbarType.Error -> SnackbarStyle(
            containerColor = Color(0xFFC62828),
            contentColor = Color(0xFFFFFFFF),
            icon = AppIcons.Snackbar.Error,
        )
        SnackbarType.Normal -> SnackbarStyle(
            containerColor = Color(0xFF616161),
            contentColor = Color(0xFFFFFFFF),
            icon = AppIcons.Snackbar.Info,
        )
    }
}

/**
 * 全页面唯一 Snackbar 宿主，挂载于 [BaseScaffold]。
 */
@Composable
fun SnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    M3SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data ->
            val visuals = data.visuals
            if (visuals is SnackbarVisuals) {
                StyledSnackbar(data = data, visuals = visuals)
            } else {
                Snackbar(snackbarData = data)
            }
        },
    )
}

@Composable
private fun StyledSnackbar(
    data: SnackbarData,
    visuals: SnackbarVisuals,
) {
    val style = snackbarStyle(visuals.type)
    Snackbar(
        modifier = Modifier.padding(horizontal = UiConstants.PageHorizontalPadding),
        shape = RoundedCornerShape(UiConstants.SnackbarCornerRadius),
        containerColor = style.containerColor,
        contentColor = style.contentColor,
        action = {
            if (visuals.actionLabel != null) {
                androidx.compose.material3.TextButton(onClick = { data.performAction() }) {
                    Text(
                        text = visuals.actionLabel,
                        color = style.contentColor,
                        fontSize = UiConstants.CompactBodyTextSize,
                    )
                }
            }
        },
        dismissAction = {
            if (visuals.withDismissAction) {
                androidx.compose.material3.IconButton(onClick = { data.dismiss() }) {
                    Icon(
                        imageVector = AppIcons.Snackbar.Dismiss,
                        contentDescription = stringResource(id = R.string.base_str_dismiss_snackbar),
                        tint = style.contentColor,
                    )
                }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                modifier = Modifier.size(UiConstants.SnackbarLeadingIconSize),
                tint = style.contentColor,
            )
            Spacer(modifier = Modifier.width(UiConstants.SnackbarLeadingIconSpacing))
            Text(
                text = visuals.message,
                fontSize = UiConstants.CompactBodyTextSize,
                color = style.contentColor,
            )
        }
    }
}
