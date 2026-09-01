package com.example.zhttaskflow.feature.task.presentation

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.ui.StateBox
import com.example.zhttaskflow.base.ui.TaskFlowScaffold
import com.example.zhttaskflow.base.ui.extension.PageLifecycleLog
import com.example.zhttaskflow.base.ui.extension.logUiInteraction
import com.example.zhttaskflow.base.ui.icon.TaskFlowIcons
import com.example.zhttaskflow.base.ui.rememberTaskFlowStateBoxContentPadding
import com.example.zhttaskflow.base.util.NetworkUtil
import com.example.zhttaskflow.feature.task.R

/**
 * 任务详情占位页：二级页标准骨架（[StateBox] 四态 + [TaskFlowScaffold] 返回拦截），
 * 成功态保留占位文案，用于验证列表 → 详情导航与系统返回栈。
 *
 * @param taskId 任务唯一标识（来自路由参数 [com.example.zhttaskflow.nav.route.TaskFlowTaskNavRoutes.ARG_TASK_ID]）
 * @param onNavigateUp 页面级返回（导航栈回退），与资讯详情一致
 */
@Composable
fun TaskDetailPlaceholderScreen(
    taskId: String,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val networkErrorMessage = stringResource(id = R.string.task_str_network_unavailable)
    val emptyMessage = stringResource(id = R.string.task_str_detail_empty)
    var detailUiState by remember(taskId) {
        mutableStateOf<BaseUiState<Unit>>(BaseUiState.Loading)
    }

    LaunchedEffect(taskId) {
        detailUiState = resolveTaskDetailUiState(
            taskId = taskId,
            context = appContext,
            networkErrorMessage = networkErrorMessage,
        )
    }

    PageLifecycleLog(
        pageName = "TaskDetail",
        pageArgs = "taskId=$taskId",
    )

    TaskFlowScaffold(
        modifier = modifier,
        title = stringResource(id = R.string.task_str_detail_title_format, taskId),
        onNavigateUp = onNavigateUp,
        navigationIcon = {
            IconButton(
                onClick = {
                    logUiInteraction(action = "click", identifier = "task_detail_back")
                    onNavigateUp()
                },
            ) {
                Icon(
                    imageVector = TaskFlowIcons.Nav.Back,
                    contentDescription = stringResource(id = R.string.task_str_back),
                )
            }
        },
    ) { _ ->
        StateBox(
            uiState = detailUiState,
            onRetry = {
                logUiInteraction(action = "click", identifier = "task_detail_retry")
                detailUiState = resolveTaskDetailUiState(
                    taskId = taskId,
                    context = appContext,
                    networkErrorMessage = networkErrorMessage,
                )
            },
            emptyMessage = emptyMessage,
            contentPadding = rememberTaskFlowStateBoxContentPadding(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = stringResource(id = R.string.task_str_detail_placeholder, taskId),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun resolveTaskDetailUiState(
    taskId: String,
    context: Context,
    networkErrorMessage: String,
): BaseUiState<Unit> {
    if (taskId.isBlank()) {
        return BaseUiState.Empty
    }
    return if (NetworkUtil.isNetworkAvailable(context)) {
        BaseUiState.Success(Unit)
    } else {
        BaseUiState.Error(networkErrorMessage)
    }
}
