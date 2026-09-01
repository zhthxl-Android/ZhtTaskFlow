package com.example.zhttaskflow.feature.task.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.zhttaskflow.base.ext.rememberTaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ext.showSnackbar
import com.example.zhttaskflow.base.extension.collectUiStateWithLifecycle
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.ui.TaskFlowSnackbarType
import com.example.zhttaskflow.base.ui.StateBox
import com.example.zhttaskflow.base.ui.TaskFlowListScaffold
import com.example.zhttaskflow.base.ui.TaskFlowPullToRefreshBox
import com.example.zhttaskflow.base.ui.TaskFlowUiConstants
import com.example.zhttaskflow.base.ui.dialog.TaskFlowConfirmDialog
import com.example.zhttaskflow.base.ui.extension.PageLifecycleLog
import com.example.zhttaskflow.base.ui.extension.logUiInteraction
import com.example.zhttaskflow.base.ui.rememberTaskFlowListLazyContentPadding
import com.example.zhttaskflow.base.ui.rememberTaskFlowListSkeletonLoading
import com.example.zhttaskflow.feature.task.R
import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 任务列表主页面：订阅状态并分发事件；消费全部页面内 UI 类 [TaskUiEffect]（导航类由路由宿主处理）。
 */
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectUiStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val lifecycleArgs = when (val state = uiState) {
        is BaseUiState.Success -> "count=${state.data.tasks.size}"
        is BaseUiState.Loading -> "loading"
        is BaseUiState.Error -> "error"
        is BaseUiState.Empty -> "empty"
    }

    PageLifecycleLog(
        pageName = "TaskList",
        pageArgs = lifecycleArgs,
    )

    TaskFlowListScaffold(
        modifier = modifier,
        collapsibleTopBarOnScroll = true,
        interceptTabRootBackToDesktop = true,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    logUiInteraction(action = "click", identifier = "task_list_fab_add")
                    showAddDialog = true
                },
            ) {
                Text(text = "+")
            }
        },
    ) { scaffoldContentPadding ->
        val snackbarDispatcher = rememberTaskFlowSnackbarDispatcher()
        LaunchedEffect(viewModel, snackbarDispatcher) {
            viewModel.uiEffect.collect { effect ->
                when (effect) {
                    is TaskUiEffect.ShowToast -> {
                        showSnackbar(
                            dispatcher = snackbarDispatcher,
                            message = effect.message,
                            type = snackbarTypeForUserFeedback(effect.message),
                        )
                    }
                    is TaskUiEffect.NavigateToEdit -> {
                        // 跨页面导航：由 TaskListRouteHost 消费，Screen 不处理
                    }
                }
            }
        }
        val listContentPadding = rememberTaskFlowListLazyContentPadding(
            scaffoldPadding = scaffoldContentPadding,
            extraBottom = TaskFlowUiConstants.FabContentExtraBottom,
        )
        TaskListContent(
            uiState = uiState,
            listContentPadding = listContentPadding,
            onRefresh = {
                logUiInteraction(action = "pullRefresh", identifier = "task_list")
                viewModel.onEvent(TaskUiEvent.Refresh)
            },
            onRetry = {
                logUiInteraction(action = "click", identifier = "task_list_retry")
                viewModel.onEvent(TaskUiEvent.Refresh)
            },
            onTaskClick = { taskId ->
                viewModel.onEvent(TaskUiEvent.TaskItemClicked(taskId))
            },
        )
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, content ->
                showAddDialog = false
                viewModel.onEvent(TaskUiEvent.AddTask(title = title, content = content))
            },
        )
    }
}

@Composable
private fun TaskListContent(
    uiState: TaskUiState,
    listContentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listSkeletonLoading = rememberTaskFlowListSkeletonLoading(listContentPadding = listContentPadding)
    StateBox(
        uiState = uiState,
        onRetry = onRetry,
        contentPadding = PaddingValues(
            horizontal = TaskFlowUiConstants.PageHorizontalPadding,
        ),
        modifier = modifier.fillMaxSize(),
        emptyMessage = stringResource(id = R.string.task_str_empty_list),
        loading = listSkeletonLoading,
    ) { data ->
        TaskRefreshableList(
            tasks = data.tasks,
            isRefreshing = data.isRefreshing,
            listContentPadding = listContentPadding,
            onRefresh = onRefresh,
            onTaskClick = onTaskClick,
        )
    }
}

/**
 * 非分页列表：统一下拉刷新 + [LazyColumn]（与 [com.example.zhttaskflow.base.ui.TaskFlowPaginatedList] 成功态刷新区一致）。
 */
@Composable
private fun TaskRefreshableList(
    tasks: List<Task>,
    isRefreshing: Boolean,
    listContentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listModifier = Modifier.fillMaxSize()
    if (tasks.isEmpty() && isRefreshing) {
        TaskFlowPullToRefreshBox(
            isRefreshing = true,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize(),
        ) {
            Box(modifier = listModifier)
        }
        return
    }

    TaskFlowPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = listModifier,
            verticalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.ListVerticalSpacing),
            contentPadding = listContentPadding,
        ) {
            items(
                items = tasks,
                key = { it.id },
            ) { task ->
                TaskListItem(
                    task = task,
                    onClick = { onTaskClick(task.id) },
                )
            }
        }
    }
}

/**
 * 单条任务列表项：标题、状态、创建时间。
 */
@Composable
private fun TaskListItem(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(TaskFlowUiConstants.PageHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = taskStatusLabel(task.status),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = formatCreatedAt(task.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    TaskFlowConfirmDialog(
        title = stringResource(id = R.string.task_str_dialog_title),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(title, content) },
        confirmText = stringResource(id = R.string.task_str_confirm),
        dismissText = stringResource(id = R.string.task_str_cancel),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.ListVerticalSpacing)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(text = stringResource(id = R.string.task_str_field_title)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(text = stringResource(id = R.string.task_str_field_content)) },
            )
        }
    }
}

@Composable
private fun taskStatusLabel(status: TaskStatus): String {
    return when (status) {
        TaskStatus.PENDING -> stringResource(id = R.string.task_str_status_pending)
        TaskStatus.IN_PROGRESS -> stringResource(id = R.string.task_str_status_in_progress)
        TaskStatus.COMPLETED -> stringResource(id = R.string.task_str_status_completed)
        TaskStatus.CANCELLED -> stringResource(id = R.string.task_str_status_cancelled)
    }
}

private fun formatCreatedAt(epochMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}

/** 按文案语义映射 Snackbar 样式（与 ViewModel [TaskUiEffect.ShowToast] 触发场景一致）。 */
private fun snackbarTypeForUserFeedback(message: String): TaskFlowSnackbarType = when (message) {
    "刷新成功", "任务已添加" -> TaskFlowSnackbarType.Success
    "请输入任务标题", "任务标识无效" -> TaskFlowSnackbarType.Error
    else -> if (message.contains("失败")) {
        TaskFlowSnackbarType.Error
    } else {
        TaskFlowSnackbarType.Normal
    }
}
