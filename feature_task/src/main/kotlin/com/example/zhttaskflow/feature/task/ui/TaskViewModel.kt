package com.example.zhttaskflow.feature.task.ui

import com.example.zhttaskflow.base.mvi.BaseViewModel
import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskRepository
import com.example.zhttaskflow.feature.task.domain.TaskStatus

/**
 * 任务列表 ViewModel：事件 → 仓库 → 状态/副作用，严格 MVI 单向数据流。
 *
 * @param repository 由外部手动构造注入，无 DI 框架。
 */
class TaskViewModel(
    private val repository: TaskRepository,
) : BaseViewModel<TaskUiState, TaskUiEvent, TaskUiEffect>(TaskUiState()) {

    private val logTag = "TaskViewModel"

    init {
        loadTasks(isRefresh = false)
    }

    override fun handleEvent(event: TaskUiEvent) {
        when (event) {
            TaskUiEvent.Refresh -> loadTasks(isRefresh = true)
            is TaskUiEvent.AddTask -> addTask(event.title, event.content)
            is TaskUiEvent.TaskItemClicked -> sendEffect(TaskUiEffect.NavigateToTaskDetail(event.taskId))
        }
    }

    private fun loadTasks(isRefresh: Boolean) {
        launchTask(
            tag = logTag,
            onError = { throwable -> applyLoadError(throwable) },
        ) {
            applyLoadingState(isRefresh)
            val tasks = repository.getAllTasks()
            applyLoadSuccess(tasks)
        }
    }

    private fun addTask(title: String, content: String) {
        if (title.isBlank()) {
            sendEffect(TaskUiEffect.ShowToast("请输入任务标题"))
            return
        }
        launchTask(
            tag = logTag,
            onError = { throwable ->
                setState { copy(errorMessage = throwable.message) }
                sendEffect(TaskUiEffect.ShowToast(throwable.message ?: "新增任务失败"))
            },
        ) {
            val task = Task(
                id = "task-${System.currentTimeMillis()}",
                title = title.trim(),
                content = content.trim(),
                createdAt = System.currentTimeMillis(),
                status = TaskStatus.PENDING,
            )
            repository.addTask(task)
            sendEffect(TaskUiEffect.ShowToast("任务已添加"))
            applyLoadingState(isRefresh = false)
            val tasks = repository.getAllTasks()
            applyLoadSuccess(tasks)
        }
    }

    private fun applyLoadingState(isRefresh: Boolean) {
        setState {
            copy(
                isLoading = if (isRefresh) isLoading else true,
                isRefreshing = isRefresh,
                errorMessage = null,
            )
        }
    }

    private fun applyLoadSuccess(tasks: List<Task>) {
        setState {
            copy(
                tasks = tasks,
                isLoading = false,
                isRefreshing = false,
                errorMessage = null,
            )
        }
    }

    private fun applyLoadError(throwable: Throwable) {
        setState {
            copy(
                isLoading = false,
                isRefreshing = false,
                errorMessage = throwable.message,
            )
        }
        sendEffect(
            TaskUiEffect.ShowToast(
                throwable.message ?: "加载任务失败",
            ),
        )
    }
}
