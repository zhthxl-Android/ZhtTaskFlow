package com.example.zhttaskflow.base.ui.dialog

import androidx.compose.runtime.Composable
import com.example.zhttaskflow.base.ext.TaskFlowDialogController
import com.example.zhttaskflow.base.ext.TaskFlowDialogPresentation

/**
 * 根据 [TaskFlowDialogController] 状态渲染全局弹窗层。
 */
@Composable
internal fun TaskFlowDialogHost(controller: TaskFlowDialogController) {
    when (val presentation = controller.presentation) {
        is TaskFlowDialogPresentation.Confirm -> {
            TaskFlowConfirmDialog(
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
        is TaskFlowDialogPresentation.BottomSheet -> {
            TaskFlowBottomSheet(
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
