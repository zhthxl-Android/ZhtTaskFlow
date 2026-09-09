package com.example.zhttaskflow.base.ui.dialog

import androidx.compose.runtime.Composable
import com.example.zhttaskflow.base.ext.DialogController
import com.example.zhttaskflow.base.ext.DialogPresentation

/**
 * 根据 [DialogController] 状态渲染全局弹窗层。
 */
@Composable
internal fun DialogHost(controller: DialogController) {
    when (val presentation = controller.presentation) {
        is DialogPresentation.Confirm -> {
            ConfirmDialog(
                title = presentation.title,
                message = presentation.message,
                confirmText = presentation.confirmText,
                dismissText = presentation.dismissText,
                onConfirm = {
                    presentation.onConfirm()
                    controller.dismissAll()
                },
                onDismiss = {
                    presentation.onDismiss()
                    controller.dismissAll()
                },
            )
        }
        is DialogPresentation.ConfirmWithContent -> {
            ConfirmDialog(
                title = presentation.title,
                confirmText = presentation.confirmText,
                dismissText = presentation.dismissText,
                onConfirm = {
                    presentation.onConfirm()
                    controller.dismissAll()
                },
                onDismiss = {
                    presentation.onDismiss()
                    controller.dismissAll()
                },
                content = presentation.content,
            )
        }
        is DialogPresentation.BottomSheet -> {
            BottomSheet(
                onDismissRequest = {
                    presentation.onDismiss()
                    controller.dismissAll()
                },
                content = presentation.content,
            )
        }
        null -> Unit
    }
}
