package com.example.zhttaskflow.feature.task.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.mvi.BaseViewModel
import com.example.zhttaskflow.base.mvi.getDataOrNull
import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskStatus
import com.example.zhttaskflow.feature.task.domain.usecase.GetTaskByIdUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.UpdateTaskUseCase

/**
 * 任务详情 ViewModel：加载、编辑、状态流转与附件交互。
 */
class TaskDetailViewModel(
    private val taskId: String,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
) : BaseViewModel<TaskDetailUiState, TaskDetailUiEvent, TaskDetailUiEffect>(BaseUiState.Loading) {

    private val logTag = "TaskDetailViewModel"

    init {
        if (taskId.isBlank()) {
            setState { BaseUiState.Empty }
        }
    }

    override fun handleEvent(event: TaskDetailUiEvent) {
        when (event) {
            is TaskDetailUiEvent.Load -> loadDetail(event.taskId)
            TaskDetailUiEvent.Retry -> loadDetail(taskId)
            TaskDetailUiEvent.StartEdit -> enterEditMode()
            TaskDetailUiEvent.CancelEdit -> cancelEdit()
            is TaskDetailUiEvent.DraftTitleChanged -> updateDraftTitle(event.value)
            is TaskDetailUiEvent.DraftContentChanged -> updateDraftContent(event.value)
            TaskDetailUiEvent.SaveEdit -> saveEdit()
            is TaskDetailUiEvent.ChangeStatus -> changeStatus(event.status)
            is TaskDetailUiEvent.AttachmentClicked -> onAttachmentClicked(event.attachmentId)
        }
    }

    private fun loadDetail(id: String) {
        if (id.isBlank()) {
            setState { BaseUiState.Empty }
            return
        }
        launchTask(
            tag = logTag,
            scene = "loadDetail",
            userMessageFallback = "加载任务详情失败，请稍后重试",
            onError = { _, message -> setState { BaseUiState.Error(message) } },
        ) {
            setState { BaseUiState.Loading }
            val task = getTaskByIdUseCase(id)
            if (task == null) {
                setState { BaseUiState.Empty }
            } else {
                setState { BaseUiState.Success(TaskDetailData(task = task)) }
            }
        }
    }

    private fun enterEditMode() {
        val data = currentState.getDataOrNull() ?: return
        if (data.isEditing) {
            return
        }
        setState {
            if (this is BaseUiState.Success) {
                BaseUiState.Success(
                    data.copy(
                        isEditing = true,
                        draftTitle = data.task.title,
                        draftContent = data.task.content,
                    ),
                )
            } else {
                this
            }
        }
    }

    private fun cancelEdit() {
        val data = currentState.getDataOrNull() ?: return
        setState {
            if (this is BaseUiState.Success) {
                BaseUiState.Success(
                    data.copy(
                        isEditing = false,
                        draftTitle = data.task.title,
                        draftContent = data.task.content,
                    ),
                )
            } else {
                this
            }
        }
        sendEffect(
            TaskDetailUiEffect.ShowSnackbar(
                message = "已取消编辑",
                type = SnackbarType.Normal,
                actionId = "task_detail_edit_cancel",
            ),
        )
    }

    private fun updateDraftTitle(value: String) {
        mutateSuccessData { copy(draftTitle = value) }
    }

    private fun updateDraftContent(value: String) {
        mutateSuccessData { copy(draftContent = value) }
    }

    private fun saveEdit() {
        val data = currentState.getDataOrNull() ?: return
        val title = data.draftTitle.trim()
        if (title.isBlank()) {
            sendEffect(
                TaskDetailUiEffect.ShowSnackbar(
                    message = "标题不能为空",
                    type = SnackbarType.Error,
                    actionId = "task_detail_save_validation",
                ),
            )
            return
        }
        val updated = data.task.copy(
            title = title,
            content = data.draftContent.trim(),
        )
        persistTaskUpdate(
            updated = updated,
            scene = "saveEdit",
            successMessage = "任务已保存",
            successActionId = "task_detail_save",
            onSuccess = { task ->
                TaskDetailData(
                    task = task,
                    isEditing = false,
                    draftTitle = task.title,
                    draftContent = task.content,
                )
            },
        )
    }

    private fun changeStatus(status: TaskStatus) {
        val data = currentState.getDataOrNull() ?: return
        if (data.task.status == status) {
            return
        }
        if (!isStatusTransitionAllowed(data.task.status, status)) {
            sendEffect(
                TaskDetailUiEffect.ShowSnackbar(
                    message = "当前状态无法直接切换为所选状态",
                    type = SnackbarType.Error,
                    actionId = "task_detail_status_invalid",
                ),
            )
            return
        }
        val updated = data.task.copy(status = status)
        persistTaskUpdate(
            updated = updated,
            scene = "changeStatus",
            successMessage = "状态已更新",
            successActionId = "task_detail_status_change",
            onSuccess = { task ->
                data.copy(
                    task = task,
                    draftTitle = task.title,
                    draftContent = task.content,
                    isEditing = false,
                )
            },
        )
    }

    private fun onAttachmentClicked(attachmentId: String) {
        val task = currentState.getDataOrNull()?.task ?: return
        val attachment = task.attachments.find { it.id == attachmentId }
        if (attachment == null) {
            sendEffect(
                TaskDetailUiEffect.ShowSnackbar(
                    message = "附件不存在",
                    type = SnackbarType.Error,
                    actionId = "task_detail_attachment_missing",
                ),
            )
            return
        }
        sendEffect(
            TaskDetailUiEffect.ShowSnackbar(
                message = "已打开附件：${attachment.displayName}（示范）",
                type = SnackbarType.Normal,
                actionId = "task_detail_attachment_open",
            ),
        )
    }

    private fun persistTaskUpdate(
        updated: Task,
        scene: String,
        successMessage: String,
        successActionId: String,
        onSuccess: (Task) -> TaskDetailData,
    ) {
        mutateSuccessData { copy(isSubmitting = true) }
        launchTask(
            tag = logTag,
            scene = scene,
            userMessageFallback = "保存失败，请稍后重试",
            onError = { _, message ->
                mutateSuccessData { copy(isSubmitting = false) }
                sendEffect(
                    TaskDetailUiEffect.ShowSnackbar(
                        message = message,
                        type = SnackbarType.Error,
                        actionId = "${successActionId}_failure",
                    ),
                )
            },
        ) {
            updateTaskUseCase(updated)
            mutateSuccessData { onSuccess(updated).copy(isSubmitting = false) }
            sendEffect(
                TaskDetailUiEffect.ShowSnackbar(
                    message = successMessage,
                    type = SnackbarType.Success,
                    actionId = successActionId,
                ),
            )
        }
    }

    private fun mutateSuccessData(reducer: TaskDetailData.() -> TaskDetailData) {
        setState {
            if (this is BaseUiState.Success) {
                BaseUiState.Success(data.reducer())
            } else {
                this
            }
        }
    }

    /**
     * 领域状态机：待处理 → 进行中 → 已完成；任意非终态可取消。
     */
    private fun isStatusTransitionAllowed(from: TaskStatus, to: TaskStatus): Boolean {
        if (from == to) {
            return false
        }
        return when (from) {
            TaskStatus.PENDING -> to == TaskStatus.IN_PROGRESS || to == TaskStatus.CANCELLED
            TaskStatus.IN_PROGRESS -> to == TaskStatus.COMPLETED || to == TaskStatus.CANCELLED
            TaskStatus.COMPLETED -> false
            TaskStatus.CANCELLED -> false
        }
    }
}

class TaskDetailViewModelFactory(
    private val taskId: String,
    private val getTaskByIdUseCase: GetTaskByIdUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskDetailViewModel::class.java)) {
            return TaskDetailViewModel(
                taskId = taskId,
                getTaskByIdUseCase = getTaskByIdUseCase,
                updateTaskUseCase = updateTaskUseCase,
            ) as T
        }
        throw IllegalArgumentException("未知 ViewModel: ${modelClass.name}")
    }
}
