package com.example.zhttaskflow.feature.task.presentation

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.zhttaskflow.base.ui.TaskFlowScaffold
import com.example.zhttaskflow.base.ui.TaskFlowUiConstants
import com.example.zhttaskflow.base.ui.icon.TaskFlowIcons
import com.example.zhttaskflow.feature.task.R

/**
 * 任务详情占位页：用于验证列表 → 详情导航与系统返回栈。
 *
 * @param taskId 任务唯一标识（来自路由参数 [com.example.zhttaskflow.nav.route.TaskFlowTaskNavRoutes.ARG_TASK_ID]）
 */
@Composable
fun TaskDetailPlaceholderScreen(
    taskId: String,
    modifier: Modifier = Modifier,
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    TaskFlowScaffold(
        modifier = modifier,
        title = stringResource(id = R.string.task_str_detail_title),
        navigationIcon = {
            IconButton(
                onClick = { backDispatcher?.onBackPressed() },
            ) {
                Icon(
                    imageVector = TaskFlowIcons.Nav.Back,
                    contentDescription = stringResource(id = R.string.task_str_back),
                )
            }
        },
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = stringResource(id = R.string.task_str_detail_placeholder, taskId),
                modifier = Modifier.padding(TaskFlowUiConstants.PageHorizontalPadding),
            )
        }
    }
}
