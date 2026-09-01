package com.example.zhttaskflow.feature.task.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.ext.TaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ext.rememberTaskFlowDialogController
import com.example.zhttaskflow.base.ext.rememberTaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ext.showBottomSheet
import com.example.zhttaskflow.base.ext.showSnackbar
import com.example.zhttaskflow.base.extension.collectUiStateWithLifecycle
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.ui.TaskFlowImeAvoidanceMode
import com.example.zhttaskflow.base.ui.TaskFlowListScaffold
import com.example.zhttaskflow.base.ui.TaskFlowRefreshableListPayload
import com.example.zhttaskflow.base.ui.TaskFlowStateRefreshableListContent
import com.example.zhttaskflow.base.ui.TaskFlowUiConstants
import com.example.zhttaskflow.base.ui.dialog.TaskFlowConfirmDialog
import com.example.zhttaskflow.base.ui.extension.PageLifecycleLog
import com.example.zhttaskflow.base.ui.extension.logUiInteraction
import com.example.zhttaskflow.base.ui.rememberTaskFlowImePadding
import com.example.zhttaskflow.base.ui.rememberTaskFlowListLazyContentPadding
import com.example.zhttaskflow.base.ui.taskFlowImeBringIntoViewOnFocus
import com.example.zhttaskflow.base.ui.taskFlowImePadding
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

    val dialogController = rememberTaskFlowDialogController()
    val snackbarDispatcher = rememberTaskFlowSnackbarDispatcher()

    TaskFlowListScaffold(
        modifier = modifier,
        title = stringResource(id = R.string.task_str_list_title),
        collapsibleTopBarOnScroll = true,
        interceptTabRootBackToDesktop = true,
        actions = {
            TextButton(
                onClick = {
                    logUiInteraction(
                        action = "click",
                        identifier = "task_list_more",
                        pageId = "TaskList",
                    )
                    showBottomSheet(controller = dialogController) {
                        TaskListMoreBottomSheetContent(
                            onItemClick = { action, feedbackMessage ->
                                dialogController.dismissAll()
                                logUiInteraction(
                                    action = "click",
                                    identifier = "task_list_more_${action.opIdSuffix}",
                                    pageId = "TaskList",
                                )
                                showSnackbar(
                                    dispatcher = snackbarDispatcher,
                                    message = feedbackMessage,
                                    type = SnackbarType.Normal,
                                )
                            },
                        )
                    }
                },
            ) {
                Text(text = stringResource(id = R.string.task_str_more))
            }
        },
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
        LaunchedEffect(viewModel, snackbarDispatcher) {
            viewModel.uiEffect.collect { effect ->
                consumeTaskListUiEffect(
                    dispatcher = snackbarDispatcher,
                    effect = effect,
                )
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

/**
 * **BottomSheet 业务接入示范**：通过 [showBottomSheet] + 全局 [com.example.zhttaskflow.base.ext.TaskFlowDialogController]
 * 渲染 [com.example.zhttaskflow.base.ui.dialog.TaskFlowBottomSheet]；选项点击后关闭并 Snackbar 反馈。
 */
private enum class TaskListMoreSheetAction(val opIdSuffix: String) {
    Batch(opIdSuffix = "batch"),
    Sort(opIdSuffix = "sort"),
    Filter(opIdSuffix = "filter"),
}

@Composable
private fun ColumnScope.TaskListMoreBottomSheetContent(
    onItemClick: (TaskListMoreSheetAction, String) -> Unit,
) {
    val menuItems = listOf(
        TaskListMoreSheetAction.Batch to
            (R.string.task_str_more_batch to R.string.task_str_more_demo_batch),
        TaskListMoreSheetAction.Sort to
            (R.string.task_str_more_sort to R.string.task_str_more_demo_sort),
        TaskListMoreSheetAction.Filter to
            (R.string.task_str_more_filter to R.string.task_str_more_demo_filter),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = TaskFlowUiConstants.PageHorizontalPadding,
                vertical = TaskFlowUiConstants.ListVerticalSpacing,
            ),
        verticalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.ListVerticalSpacing),
    ) {
        menuItems.forEach { (action, labels) ->
            val label = stringResource(id = labels.first)
            val feedbackMessage = stringResource(id = labels.second)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = TaskFlowUiConstants.DialogActionHeight)
                    .clickable { onItemClick(action, feedbackMessage) }
                    .padding(vertical = TaskFlowUiConstants.ListVerticalSpacing),
            )
        }
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
    TaskFlowStateRefreshableListContent(
        uiState = uiState.toRefreshableUiState(),
        onRetry = onRetry,
        onRefresh = onRefresh,
        listContentPadding = listContentPadding,
        modifier = modifier.fillMaxSize(),
        emptyMessage = stringResource(id = R.string.task_str_empty_list),
        key = { _, task -> task.id },
    ) { _, task ->
        TaskListItem(
            task = task,
            onClick = { onTaskClick(task.id) },
        )
    }
}

private fun TaskUiState.toRefreshableUiState(): BaseUiState<TaskFlowRefreshableListPayload<Task>> =
    when (this) {
        BaseUiState.Loading -> BaseUiState.Loading
        BaseUiState.Empty -> BaseUiState.Empty
        is BaseUiState.Error -> BaseUiState.Error(message = message)
        is BaseUiState.Success -> BaseUiState.Success(
            data = TaskFlowRefreshableListPayload(
                items = data.tasks,
                isRefreshing = data.isRefreshing,
            ),
        )
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
    val imePadding = rememberTaskFlowImePadding(mode = TaskFlowImeAvoidanceMode.BringIntoView)

    TaskFlowConfirmDialog(
        title = stringResource(id = R.string.task_str_dialog_title),
        onDismiss = onDismiss,
        onConfirm = { onConfirm(title, content) },
        confirmText = stringResource(id = R.string.task_str_confirm),
        dismissText = stringResource(id = R.string.task_str_cancel),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .taskFlowImePadding(imePadding),
            verticalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.ListVerticalSpacing),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(text = stringResource(id = R.string.task_str_field_title)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .taskFlowImeBringIntoViewOnFocus(),
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(text = stringResource(id = R.string.task_str_field_content)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .taskFlowImeBringIntoViewOnFocus(),
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

/**
 * Screen 层 Collector：仅处理 [com.example.zhttaskflow.base.ext.TaskFlowPresentationUiEffect]。
 * 导航类 Effect 由 [com.example.zhttaskflow.feature.task.navigation.TaskListRouteHost] 消费，见 [com.example.zhttaskflow.base.ext.TaskFlowUiEffectConsumption]。
 */
private fun consumeTaskListUiEffect(
    dispatcher: TaskFlowSnackbarDispatcher,
    effect: TaskUiEffect,
) {
    when (effect) {
        is TaskUiEffect.ShowSnackbar -> {
            showSnackbar(
                dispatcher = dispatcher,
                message = effect.message,
                type = effect.type,
            )
        }
        is TaskUiEffect.NavigateToEdit -> {
            // TaskFlowNavigationUiEffect：由 TaskListRouteHost 消费
        }
    }
}
