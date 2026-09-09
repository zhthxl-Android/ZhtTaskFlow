package com.example.zhttaskflow.base.ui.dialog

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.zhttaskflow.base.R
import com.example.zhttaskflow.base.ui.UiConstants

/**
 * 标准确认弹窗：标题、正文、确认/取消，样式与主题对齐。
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String? = null,
    dismissText: String? = null,
) {
    val resolvedConfirm = confirmText ?: stringResource(id = R.string.base_str_confirm)
    val resolvedDismiss = dismissText ?: stringResource(id = R.string.base_str_cancel)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(UiConstants.DialogCornerRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = UiConstants.CompactBodyTextSize,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = UiConstants.DialogActionHeight),
            ) {
                Text(
                    text = resolvedConfirm,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = UiConstants.DialogActionHeight),
            ) {
                Text(
                    text = resolvedDismiss,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

/**
 * 带自定义正文区域的确认弹窗（圆角、配色与纯文案 [ConfirmDialog] 一致），适用于简易表单等场景。
 */
@Composable
fun ConfirmDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String? = null,
    dismissText: String? = null,
    content: @Composable () -> Unit,
) {
    val resolvedConfirm = confirmText ?: stringResource(id = R.string.base_str_confirm)
    val resolvedDismiss = dismissText ?: stringResource(id = R.string.base_str_cancel)
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(UiConstants.DialogCornerRadius),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = content,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.heightIn(min = UiConstants.DialogActionHeight),
            ) {
                Text(
                    text = resolvedConfirm,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = UiConstants.DialogActionHeight),
            ) {
                Text(
                    text = resolvedDismiss,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}
