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
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.ext.SnackbarDispatcher
import com.example.zhttaskflow.base.ext.rememberDialogController
import com.example.zhttaskflow.base.ext.rememberSnackbarDispatcher
import com.example.zhttaskflow.base.ext.showBottomSheet
import com.example.zhttaskflow.base.ext.showSnackbar
import com.example.zhttaskflow.base.extension.collectUiStateWithLifecycle
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.ui.ImeAvoidanceMode
import com.example.zhttaskflow.base.ui.ListScaffold
import com.example.zhttaskflow.base.ui.RefreshableListPayload
import com.example.zhttaskflow.base.ui.rememberStateBoxContentPadding
import com.example.zhttaskflow.base.ui.StateRefreshableListContent
import com.example.zhttaskflow.base.ui.UiConstants
import com.example.zhttaskflow.base.ui.extension.PageLifecycleLog
import com.example.zhttaskflow.base.ui.extension.listItemClickWithLog
import com.example.zhttaskflow.base.ui.extension.logUiInteraction
import com.example.zhttaskflow.base.ui.extension.logUiOutcome
import com.example.zhttaskflow.base.ui.rememberImePadding
import com.example.zhttaskflow.base.ui.rememberListLazyContentPadding
import com.example.zhttaskflow.base.ui.imeBringIntoViewOnFocus
import com.example.zhttaskflow.base.ui.imePadding
import com.example.zhttaskflow.feature.task.R
import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TASK_LIST_PAGE_ID: String = "TaskList"

/**
 * 任务列表主页面：订阅状态并分发事件；消费全部页面内 UI 类 [TaskUiEffect]（导航类由路由宿主处理）。
 */
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectUiStateWithLifecycle()
    val addTaskFormState = remember { AddTaskFormState() }
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

    val dialogController = rememberDialogController()
    val snackbarDispatcher = rememberSnackbarDispatcher()
    val addDialogTitle = stringResource(id = R.string.task_str_dialog_title)
    val addDialogConfirmText = stringResource(id = R.string.task_str_confirm)
    val addDialogDismissText = stringResource(id = R.string.task_str_cancel)

    ListScaffold(
        modifier = modifier,
        title = stringResource(id = R.string.task_str_list_title),
        collapsibleTopBarOnScroll = true,
        interceptTabRootBackToDesktop = false,
        actions = {
            TextButton(
                onClick = {
                    logUiInteraction(
                        action = "click",
                        identifier = "task_list_more",
                        pageId = TASK_LIST_PAGE_ID,
                    )
                    showBottomSheet(controller = dialogController) {
                        TaskListMoreBottomSheetContent(
                            onItemClick = { action, feedbackMessage ->
                                dialogController.dismissAll()
                                logUiInteraction(
                                    action = "click",
                                    identifier = "task_list_more_${action.actionSuffix}",
                                    pageId = TASK_LIST_PAGE_ID,
                                )
                                logUiOutcome(
                                    pageId = TASK_LIST_PAGE_ID,
                                    actionId = "task_list_more_result",
                                    outcome = "info",
                                    params = mapOf(
                                        "message" to feedbackMessage,
                                        "option" to action.actionSuffix,
                                    ),
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
                    logUiInteraction(
                        action = "click",
                        identifier = "task_list_fab_add",
                        pageId = TASK_LIST_PAGE_ID,
                    )
                    addTaskFormState.reset()
                    dialogController.showConfirmDialog(
                        title = addDialogTitle,
                        confirmText = addDialogConfirmText,
                        dismissText = addDialogDismissText,
                        onDismiss = { addTaskFormState.reset() },
                        onConfirm = {
                            viewModel.onEvent(
                                TaskUiEvent.AddTask(
                                    title = addTaskFormState.title,
                                    content = addTaskFormState.content,
                                ),
                            )
                            addTaskFormState.reset()
                        },
                    ) {
                        AddTaskDialogFormContent(formState = addTaskFormState)
                    }
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
        val listContentPadding = rememberListLazyContentPadding(
            scaffoldPadding = scaffoldContentPadding,
            extraBottom = UiConstants.FabContentExtraBottom,
        )
        TaskListContent(
            uiState = uiState,
            listContentPadding = listContentPadding,
            onRefresh = {
                logUiInteraction(
                    action = "pullRefresh",
                    identifier = "task_list",
                    pageId = TASK_LIST_PAGE_ID,
                )
                viewModel.onEvent(TaskUiEvent.Refresh)
            },
            onRetry = {
                logUiInteraction(
                    action = "click",
                    identifier = "task_list_retry",
                    pageId = TASK_LIST_PAGE_ID,
                )
                viewModel.onEvent(TaskUiEvent.Refresh)
            },
            onTaskClick = { taskId ->
                viewModel.onEvent(TaskUiEvent.TaskItemClicked(taskId))
            },
        )
    }
}

private class AddTaskFormState {
    var title by mutableStateOf("")
    var content by mutableStateOf("")

    fun reset() {
        title = ""
        content = ""
    }
}

/**
 * **BottomSheet 业务接入示范**：通过 [showBottomSheet] + 全局 [com.example.zhttaskflow.base.ext.DialogController]
 * 渲染 [com.example.zhttaskflow.base.ui.dialog.BottomSheet]；选项点击后关闭并 Snackbar 反馈。
 */
private enum class TaskListMoreSheetAction(val actionSuffix: String) {
    Batch(actionSuffix = "batch"),
    Sort(actionSuffix = "sort"),
    Filter(actionSuffix = "filter"),
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
                horizontal = UiConstants.PageHorizontalPadding,
                vertical = UiConstants.ListVerticalSpacing,
            ),
        verticalArrangement = Arrangement.spacedBy(UiConstants.ListVerticalSpacing),
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
                    .heightIn(min = UiConstants.DialogActionHeight)
                    .clickable { onItemClick(action, feedbackMessage) }
                    .padding(vertical = UiConstants.ListVerticalSpacing),
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
    StateRefreshableListContent(
        uiState = uiState.toRefreshableUiState(),
        onRetry = onRetry,
        onRefresh = onRefresh,
        listContentPadding = listContentPadding,
        modifier = modifier.fillMaxSize(),
        contentPadding = rememberStateBoxContentPadding(),
        emptyMessage = stringResource(id = R.string.task_str_empty_list),
        key = { _, task -> task.id },
    ) { index, task ->
        TaskListItem(
            task = task,
            index = index,
            onClick = { onTaskClick(task.id) },
        )
    }
}

private fun TaskUiState.toRefreshableUiState(): BaseUiState<RefreshableListPayload<Task>> =
    when (this) {
        BaseUiState.Loading -> BaseUiState.Loading
        BaseUiState.Empty -> BaseUiState.Empty
        is BaseUiState.Error -> BaseUiState.Error(message = message)
        is BaseUiState.Success -> BaseUiState.Success(
            data = RefreshableListPayload(
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
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .listItemClickWithLog(
                identifier = "task_list_item",
                index = index,
                pageId = TASK_LIST_PAGE_ID,
                params = mapOf("taskId" to task.id),
                onClick = onClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(UiConstants.PageHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(UiConstants.ListItemCompactVerticalSpacing),
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
private fun AddTaskDialogFormContent(
    formState: AddTaskFormState,
) {
    val imePadding = rememberImePadding(mode = ImeAvoidanceMode.BringIntoView)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(imePadding),
        verticalArrangement = Arrangement.spacedBy(UiConstants.ListVerticalSpacing),
    ) {
        OutlinedTextField(
            value = formState.title,
            onValueChange = { formState.title = it },
            label = { Text(text = stringResource(id = R.string.task_str_field_title)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .imeBringIntoViewOnFocus(),
        )
        OutlinedTextField(
            value = formState.content,
            onValueChange = { formState.content = it },
            label = { Text(text = stringResource(id = R.string.task_str_field_content)) },
            modifier = Modifier
                .fillMaxWidth()
                .imeBringIntoViewOnFocus(),
        )
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
 * Screen 层 Collector：仅处理 [com.example.zhttaskflow.base.ext.PresentationUiEffect]。
 * 导航类 Effect 由 [com.example.zhttaskflow.feature.task.navigation.TaskListRouteHost] 消费，见 [com.example.zhttaskflow.base.ext.UiEffectConsumption]。
 */
private fun consumeTaskListUiEffect(
    dispatcher: SnackbarDispatcher,
    effect: TaskUiEffect,
) {
    when (effect) {
        is TaskUiEffect.ShowSnackbar -> {
            val outcome = when (effect.type) {
                SnackbarType.Success -> "success"
                SnackbarType.Error -> "failure"
                SnackbarType.Normal -> "info"
            }
            logUiOutcome(
                pageId = TASK_LIST_PAGE_ID,
                actionId = "task_list_snackbar",
                outcome = outcome,
                params = mapOf("message" to effect.message),
            )
            showSnackbar(
                dispatcher = dispatcher,
                message = effect.message,
                type = effect.type,
            )
        }
        is TaskUiEffect.NavigateToEdit -> {
            // NavigationUiEffect：由 TaskListRouteHost 消费
        }
    }
}
