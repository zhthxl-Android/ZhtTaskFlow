package com.example.zhttaskflow.feature.task.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.ext.rememberSnackbarDispatcher
import com.example.zhttaskflow.base.ext.showSnackbar
import com.example.zhttaskflow.base.extension.collectUiStateWithLifecycle
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.ui.StateBox
import com.example.zhttaskflow.base.ui.PageScaffold
import com.example.zhttaskflow.base.ui.UiConstants
import com.example.zhttaskflow.base.ui.extension.PageLifecycleLog
import com.example.zhttaskflow.base.ui.extension.logUiInteraction
import com.example.zhttaskflow.base.ui.extension.logUiOutcome
import com.example.zhttaskflow.base.ui.icon.AppIcons
import com.example.zhttaskflow.base.ui.rememberStateBoxContentPadding
import com.example.zhttaskflow.base.ui.skeleton.rememberDetailSkeletonLoading
import com.example.zhttaskflow.feature.task.R
import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskAttachment
import com.example.zhttaskflow.feature.task.domain.TaskStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 任务详情页埋点 pageId（与全局 Analytics 约定一致）。 */
internal const val TASK_DETAIL_PAGE_ID: String = "TaskDetail"

/**
 * 任务详情页：二级页标准骨架（[StateBox] 四态 + [PageScaffold] 返回拦截），
 * 展示任务信息、状态流转、编辑与附件列表。
 *
 * @param taskId 路由参数 [com.example.zhttaskflow.nav.route.TaskNavRoutes.ARG_TASK_ID]
 * @param onNavigateUp 导航栈回退
 */
@Composable
fun TaskDetailPlaceholderScreen(
    viewModel: TaskDetailViewModel,
    taskId: String,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectUiStateWithLifecycle()
    val snackbarDispatcher = rememberSnackbarDispatcher()
    val emptyMessage = stringResource(id = R.string.task_str_detail_empty)
    val defaultTitle = stringResource(id = R.string.task_str_detail_title)
    val scaffoldTitle = when (val state = uiState) {
        is BaseUiState.Success -> state.data.task.title.ifBlank { defaultTitle }
        else -> stringResource(id = R.string.task_str_detail_title_format, taskId)
    }
    val lifecycleArgs = when (val state = uiState) {
        is BaseUiState.Success -> "taskId=${state.data.task.id};status=${state.data.task.status}"
        is BaseUiState.Loading -> "loading"
        is BaseUiState.Error -> "error"
        is BaseUiState.Empty -> "empty"
    }
    val detailSkeletonLoading = rememberDetailSkeletonLoading()

    LaunchedEffect(taskId) {
        viewModel.onEvent(TaskDetailUiEvent.Load(taskId))
    }

    LaunchedEffect(viewModel, snackbarDispatcher) {
        viewModel.uiEffect.collect { effect ->
            consumeTaskDetailUiEffect(
                dispatcher = snackbarDispatcher,
                effect = effect,
            )
        }
    }

    PageLifecycleLog(
        pageName = TASK_DETAIL_PAGE_ID,
        pageArgs = lifecycleArgs,
    )

    PageScaffold(
        modifier = modifier,
        title = scaffoldTitle,
        onNavigateUp = onNavigateUp,
        navigationIcon = {
            IconButton(
                onClick = {
                    logUiInteraction(
                        action = "click",
                        identifier = "task_detail_back",
                        pageId = TASK_DETAIL_PAGE_ID,
                    )
                    onNavigateUp()
                },
            ) {
                Icon(
                    imageVector = AppIcons.Nav.Back,
                    contentDescription = stringResource(id = R.string.task_str_back),
                )
            }
        },
        actions = {
            val detailData = (uiState as? BaseUiState.Success)?.data
            if (detailData != null && !detailData.isEditing && !detailData.isSubmitting) {
                TextButton(
                    onClick = {
                        logUiInteraction(
                            action = "click",
                            identifier = "task_detail_edit",
                            pageId = TASK_DETAIL_PAGE_ID,
                        )
                        viewModel.onEvent(TaskDetailUiEvent.StartEdit)
                    },
                ) {
                    Text(text = stringResource(id = R.string.task_str_detail_edit))
                }
            }
        },
    ) { _ ->
        StateBox(
            uiState = uiState,
            onRetry = {
                logUiInteraction(
                    action = "click",
                    identifier = "task_detail_retry",
                    pageId = TASK_DETAIL_PAGE_ID,
                )
                viewModel.onEvent(TaskDetailUiEvent.Retry)
            },
            emptyMessage = emptyMessage,
            loading = detailSkeletonLoading,
            contentPadding = rememberStateBoxContentPadding(),
            modifier = Modifier.fillMaxSize(),
        ) { data ->
            TaskDetailSuccessContent(
                data = data,
                onDraftTitleChange = { value ->
                    viewModel.onEvent(TaskDetailUiEvent.DraftTitleChanged(value))
                },
                onDraftContentChange = { value ->
                    viewModel.onEvent(TaskDetailUiEvent.DraftContentChanged(value))
                },
                onSaveEdit = {
                    logUiInteraction(
                        action = "click",
                        identifier = "task_detail_save",
                        pageId = TASK_DETAIL_PAGE_ID,
                    )
                    viewModel.onEvent(TaskDetailUiEvent.SaveEdit)
                },
                onCancelEdit = {
                    logUiInteraction(
                        action = "click",
                        identifier = "task_detail_cancel_edit",
                        pageId = TASK_DETAIL_PAGE_ID,
                    )
                    viewModel.onEvent(TaskDetailUiEvent.CancelEdit)
                },
                onStatusSelected = { status ->
                    logUiInteraction(
                        action = "click",
                        identifier = "task_detail_status_${status.name.lowercase()}",
                        pageId = TASK_DETAIL_PAGE_ID,
                        params = mapOf("taskId" to data.task.id),
                    )
                    viewModel.onEvent(TaskDetailUiEvent.ChangeStatus(status))
                },
                onAttachmentClick = { attachment ->
                    logUiInteraction(
                        action = "click",
                        identifier = "task_detail_attachment",
                        pageId = TASK_DETAIL_PAGE_ID,
                        params = mapOf(
                            "taskId" to data.task.id,
                            "attachmentId" to attachment.id,
                        ),
                    )
                    viewModel.onEvent(TaskDetailUiEvent.AttachmentClicked(attachment.id))
                },
            )
        }
    }
}

@Composable
private fun TaskDetailSuccessContent(
    data: TaskDetailData,
    onDraftTitleChange: (String) -> Unit,
    onDraftContentChange: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onStatusSelected: (TaskStatus) -> Unit,
    onAttachmentClick: (TaskAttachment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = UiConstants.ListVerticalSpacing),
        verticalArrangement = Arrangement.spacedBy(UiConstants.ListVerticalSpacing),
    ) {
        TaskDetailInfoSection(
            task = data.task,
            isEditing = data.isEditing,
            draftTitle = data.draftTitle,
            draftContent = data.draftContent,
            isSubmitting = data.isSubmitting,
            onDraftTitleChange = onDraftTitleChange,
            onDraftContentChange = onDraftContentChange,
            onSaveEdit = onSaveEdit,
            onCancelEdit = onCancelEdit,
        )
        TaskDetailStatusSection(
            currentStatus = data.task.status,
            enabled = !data.isEditing && !data.isSubmitting,
            onStatusSelected = onStatusSelected,
        )
        TaskDetailAttachmentSection(
            attachments = data.task.attachments,
            enabled = !data.isSubmitting,
            onAttachmentClick = onAttachmentClick,
        )
    }
}

@Composable
private fun TaskDetailInfoSection(
    task: Task,
    isEditing: Boolean,
    draftTitle: String,
    draftContent: String,
    isSubmitting: Boolean,
    onDraftTitleChange: (String) -> Unit,
    onDraftContentChange: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    DetailSectionCard(title = stringResource(id = R.string.task_str_detail_section_info)) {
        if (isEditing) {
            OutlinedTextField(
                value = draftTitle,
                onValueChange = onDraftTitleChange,
                label = { Text(text = stringResource(id = R.string.task_str_field_title)) },
                singleLine = true,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draftContent,
                onValueChange = onDraftContentChange,
                label = { Text(text = stringResource(id = R.string.task_str_field_content)) },
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancelEdit, enabled = !isSubmitting) {
                    Text(text = stringResource(id = R.string.task_str_cancel))
                }
                TextButton(onClick = onSaveEdit, enabled = !isSubmitting) {
                    Text(text = stringResource(id = R.string.task_str_confirm))
                }
            }
        } else {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = task.content.ifBlank { stringResource(id = R.string.task_str_detail_content_empty) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    id = R.string.task_str_detail_created_at,
                    formatCreatedAt(task.createdAt),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskDetailStatusSection(
    currentStatus: TaskStatus,
    enabled: Boolean,
    onStatusSelected: (TaskStatus) -> Unit,
) {
    DetailSectionCard(title = stringResource(id = R.string.task_str_detail_section_status)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(UiConstants.DetailContentBlockSpacing),
            verticalArrangement = Arrangement.spacedBy(UiConstants.DetailContentBlockSpacing),
        ) {
            TaskStatus.entries.forEach { status ->
                FilterChip(
                    selected = currentStatus == status,
                    onClick = { onStatusSelected(status) },
                    enabled = enabled,
                    label = { Text(text = taskStatusLabel(status)) },
                )
            }
        }
    }
}

@Composable
private fun TaskDetailAttachmentSection(
    attachments: List<TaskAttachment>,
    enabled: Boolean,
    onAttachmentClick: (TaskAttachment) -> Unit,
) {
    DetailSectionCard(title = stringResource(id = R.string.task_str_detail_section_attachments)) {
        if (attachments.isEmpty()) {
            Text(
                text = stringResource(id = R.string.task_str_detail_attachments_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(UiConstants.DetailContentBlockSpacing)) {
                attachments.forEach { attachment ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (enabled) {
                                    Modifier.clickableWithLog(attachment, onAttachmentClick)
                                } else {
                                    Modifier
                                },
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(UiConstants.PageHorizontalPadding),
                            verticalArrangement = Arrangement.spacedBy(
                                UiConstants.ListItemCompactVerticalSpacing,
                            ),
                        ) {
                            Text(
                                text = attachment.displayName,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = stringResource(
                                    id = R.string.task_str_detail_attachment_meta,
                                    formatAttachmentSize(attachment.sizeBytes),
                                    attachment.mimeType,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = UiConstants.PageHorizontalPadding),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(UiConstants.PageHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(UiConstants.ListVerticalSpacing),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            content()
        }
    }
}

@Composable
private fun Modifier.clickableWithLog(
    attachment: TaskAttachment,
    onClick: (TaskAttachment) -> Unit,
): Modifier {
    return this.then(
        Modifier.clickable(
            onClick = { onClick(attachment) },
        ),
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

private fun formatAttachmentSize(sizeBytes: Long): String {
    return when {
        sizeBytes < 1024 -> "${sizeBytes}B"
        sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024}KB"
        else -> String.format(Locale.getDefault(), "%.1fMB", sizeBytes / (1024.0 * 1024.0))
    }
}

private fun consumeTaskDetailUiEffect(
    dispatcher: com.example.zhttaskflow.base.ext.SnackbarDispatcher,
    effect: TaskDetailUiEffect,
) {
    when (effect) {
        is TaskDetailUiEffect.ShowSnackbar -> {
            val outcome = when (effect.type) {
                SnackbarType.Success -> "success"
                SnackbarType.Error -> "failure"
                SnackbarType.Normal -> "info"
            }
            logUiOutcome(
                pageId = TASK_DETAIL_PAGE_ID,
                actionId = effect.actionId,
                outcome = outcome,
                params = mapOf("message" to effect.message),
            )
            showSnackbar(
                dispatcher = dispatcher,
                message = effect.message,
                type = effect.type,
            )
        }
    }
}
