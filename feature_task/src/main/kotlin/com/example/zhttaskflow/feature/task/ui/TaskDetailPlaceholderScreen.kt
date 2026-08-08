package com.example.zhttaskflow.feature.task.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.zhttaskflow.feature.task.R

/**
 * 任务详情占位页：用于验证列表 → 详情导航与系统返回栈。
 *
 * @param taskId 任务唯一标识（来自路由参数 [com.example.zhttaskflow.feature.task.navigation.TaskRoute.ARG_TASK_ID]）
 */
@Composable
fun TaskDetailPlaceholderScreen(
    taskId: String,
    modifier: Modifier = Modifier,
) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.task_str_detail_title))
                },
                navigationIcon = {
                    TextButton(
                        onClick = { backDispatcher?.onBackPressed() },
                    ) {
                        Text(text = stringResource(id = R.string.task_str_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Text(
            text = stringResource(id = R.string.task_str_detail_placeholder, taskId),
            modifier = Modifier.padding(innerPadding).padding(16.dp),
        )
    }
}
