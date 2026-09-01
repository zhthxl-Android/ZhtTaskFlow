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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zhttaskflow.base.R
import com.example.zhttaskflow.base.ui.icon.TaskFlowIcons

/**
 * 全局 Snackbar 预设类型：成功 / 错误 / 普通，对应配色、图标与默认展示时长。
 */
enum class TaskFlowSnackbarType {
    Success,
    Error,
    Normal,
}

/**
 * 带类型的 [SnackbarVisuals]，供 [TaskFlowSnackbarHost] 渲染统一样式。
 */
@Immutable
data class TaskFlowSnackbarVisuals(
    override val message: String,
    val type: TaskFlowSnackbarType,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = type.defaultDuration,
) : SnackbarVisuals

val TaskFlowSnackbarType.defaultDuration: SnackbarDuration
    get() = when (this) {
        TaskFlowSnackbarType.Success -> SnackbarDuration.Short
        TaskFlowSnackbarType.Error -> SnackbarDuration.Long
        TaskFlowSnackbarType.Normal -> SnackbarDuration.Short
    }

@Immutable
private data class TaskFlowSnackbarStyle(
    val containerColor: Color,
    val contentColor: Color,
    val icon: ImageVector,
)

private fun taskFlowSnackbarStyle(type: TaskFlowSnackbarType): TaskFlowSnackbarStyle {
    return when (type) {
        TaskFlowSnackbarType.Success -> TaskFlowSnackbarStyle(
            containerColor = Color(0xFF2E7D32),
            contentColor = Color(0xFFFFFFFF),
            icon = TaskFlowIcons.Snackbar.Success,
        )
        TaskFlowSnackbarType.Error -> TaskFlowSnackbarStyle(
            containerColor = Color(0xFFC62828),
            contentColor = Color(0xFFFFFFFF),
            icon = TaskFlowIcons.Snackbar.Error,
        )
        TaskFlowSnackbarType.Normal -> TaskFlowSnackbarStyle(
            containerColor = Color(0xFF616161),
            contentColor = Color(0xFFFFFFFF),
            icon = TaskFlowIcons.Snackbar.Info,
        )
    }
}

/**
 * 全页面唯一 Snackbar 宿主，挂载于 [TaskFlowBaseScaffold]。
 */
@Composable
fun TaskFlowSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data ->
            val visuals = data.visuals
            if (visuals is TaskFlowSnackbarVisuals) {
                TaskFlowStyledSnackbar(data = data, visuals = visuals)
            } else {
                Snackbar(snackbarData = data)
            }
        },
    )
}

@Composable
private fun TaskFlowStyledSnackbar(
    data: SnackbarData,
    visuals: TaskFlowSnackbarVisuals,
) {
    val style = taskFlowSnackbarStyle(visuals.type)
    Snackbar(
        modifier = Modifier.padding(horizontal = TaskFlowUiConstants.PageHorizontalPadding),
        shape = RoundedCornerShape(TaskFlowUiConstants.SnackbarCornerRadius),
        containerColor = style.containerColor,
        contentColor = style.contentColor,
        action = {
            if (visuals.actionLabel != null) {
                androidx.compose.material3.TextButton(onClick = { data.performAction() }) {
                    Text(
                        text = visuals.actionLabel,
                        color = style.contentColor,
                        fontSize = 14.sp,
                    )
                }
            }
        },
        dismissAction = {
            if (visuals.withDismissAction) {
                androidx.compose.material3.IconButton(onClick = { data.dismiss() }) {
                    Icon(
                        imageVector = TaskFlowIcons.Snackbar.Dismiss,
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
                modifier = Modifier.size(20.dp),
                tint = style.contentColor,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = visuals.message,
                fontSize = 14.sp,
                color = style.contentColor,
            )
        }
    }
}
