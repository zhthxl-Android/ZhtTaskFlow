package com.example.zhttaskflow.feature.task.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.zhttaskflow.base.extension.collectUiStateWithLifecycle
import com.example.zhttaskflow.base.ui.state.BaseEmptyScreen
import com.example.zhttaskflow.base.ui.state.BaseErrorScreen
import com.example.zhttaskflow.base.ui.state.BaseLoadingScreen
import com.example.zhttaskflow.feature.task.R
import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 任务列表主页面：订阅 [TaskViewModel] 状态与副作用，纯声明式 UI，不直接接触数据层与导航实现。
 *
 * @param viewModel 由路由宿主注入的 ViewModel（已持有 [com.example.zhttaskflow.nav.TaskFlowNavigator]）
 */
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectUiStateWithLifecycle()
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is TaskUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.task_str_list_title))
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
            ) {
                Text(text = "+")
            }
        },
    ) { innerPadding ->
        TaskListContent(
            uiState = uiState,
            contentPadding = innerPadding,
            onRefresh = { viewModel.onEvent(TaskUiEvent.Refresh) },
            onRetry = { viewModel.onEvent(TaskUiEvent.Refresh) },
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
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onTaskClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading && uiState.tasks.isEmpty() -> {
            BaseLoadingScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
        uiState.errorMessage != null && uiState.tasks.isEmpty() -> {
            BaseErrorScreen(
                message = uiState.errorMessage ?: stringResource(id = R.string.task_str_load_failed),
                onRetry = onRetry,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
        uiState.isEmpty -> {
            BaseEmptyScreen(
                message = stringResource(id = R.string.task_str_empty_list),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = uiState.tasks,
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
            modifier = Modifier.padding(16.dp),
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.task_str_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title, content) }) {
                Text(text = stringResource(id = R.string.task_str_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.task_str_cancel))
            }
        },
    )
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
