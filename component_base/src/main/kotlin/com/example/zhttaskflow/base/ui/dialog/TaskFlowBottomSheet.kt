package com.example.zhttaskflow.base.ui.dialog

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import com.example.zhttaskflow.base.ui.TaskFlowUiConstants

/**
 * 统一底部弹窗容器：顶部圆角、拖拽条、手势下滑关闭与系统进出动画。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFlowBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = RoundedCornerShape(
            topStart = TaskFlowUiConstants.BottomSheetTopCornerRadius,
            topEnd = TaskFlowUiConstants.BottomSheetTopCornerRadius,
        ),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                modifier = Modifier.width(TaskFlowUiConstants.BottomSheetDragHandleWidth),
            )
        },
        content = content,
    )
}
