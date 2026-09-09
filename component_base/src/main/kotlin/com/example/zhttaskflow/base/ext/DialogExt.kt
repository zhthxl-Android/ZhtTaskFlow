package com.example.zhttaskflow.base.ext

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.zhttaskflow.base.ui.dialog.BottomSheet
import com.example.zhttaskflow.base.ui.dialog.ConfirmDialog

/**
 * 由 [com.example.zhttaskflow.base.ui.BaseScaffold] 注入的全局弹窗控制器。
 */
val LocalDialogController = compositionLocalOf<DialogController> {
    error("DialogController 未提供，请使用 BaseScaffold 包裹页面")
}

sealed interface DialogPresentation {
    data class Confirm(
        val title: String,
        val message: String,
        val confirmText: String? = null,
        val dismissText: String? = null,
        val onConfirm: () -> Unit = {},
        val onDismiss: () -> Unit = {},
    ) : DialogPresentation

    /**
     * 带自定义正文（如表单）的确认弹窗，由 [DialogHost] 渲染与纯文案 [Confirm] 一致的 [ConfirmDialog] 样式。
     */
    data class ConfirmWithContent(
        val title: String,
        val confirmText: String? = null,
        val dismissText: String? = null,
        val onConfirm: () -> Unit = {},
        val onDismiss: () -> Unit = {},
        val content: @Composable () -> Unit,
    ) : DialogPresentation

    data class BottomSheet(
        val onDismiss: () -> Unit = {},
        val content: @Composable ColumnScope.() -> Unit,
    ) : DialogPresentation
}

/**
 * 管理确认弹窗与底部弹窗的显隐；离开页面时由脚手架自动 [dismissAll]。
 */
@Stable
class DialogController internal constructor() {
    var presentation by mutableStateOf<DialogPresentation?>(null)
        private set

    fun showConfirmDialog(
        title: String,
        message: String,
        confirmText: String? = null,
        dismissText: String? = null,
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        presentation = DialogPresentation.Confirm(
            title = title,
            message = message,
            confirmText = confirmText,
            dismissText = dismissText,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }

    /**
     * 展示带自定义正文的确认弹窗（样式与 [showConfirmDialog] 一致，纳入同一全局队列）。
     */
    fun showConfirmDialog(
        title: String,
        confirmText: String? = null,
        dismissText: String? = null,
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {},
        content: @Composable () -> Unit,
    ) {
        presentation = DialogPresentation.ConfirmWithContent(
            title = title,
            confirmText = confirmText,
            dismissText = dismissText,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            content = content,
        )
    }

    fun showBottomSheet(
        onDismiss: () -> Unit = {},
        content: @Composable ColumnScope.() -> Unit,
    ) {
        presentation = DialogPresentation.BottomSheet(
            onDismiss = onDismiss,
            content = content,
        )
    }

    fun dismissAll() {
        presentation = null
    }
}

@Composable
fun rememberDialogController(): DialogController {
    return LocalDialogController.current
}

/**
 * 展示标准确认弹窗。
 */
fun showConfirmDialog(
    controller: DialogController,
    title: String,
    message: String,
    confirmText: String? = null,
    dismissText: String? = null,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    controller.showConfirmDialog(
        title = title,
        message = message,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/**
 * 展示底部弹窗容器。
 */
fun showBottomSheet(
    controller: DialogController,
    onDismiss: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    controller.showBottomSheet(onDismiss = onDismiss, content = content)
}
