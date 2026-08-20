package com.example.zhttaskflow.feature.task.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.mvi.BaseViewModel
import com.example.zhttaskflow.base.mvi.getDataOrNull
import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskRepository
import com.example.zhttaskflow.feature.task.domain.TaskStatus
import com.example.zhttaskflow.feature.task.navigation.TaskRoute

/**
 * 任务列表 ViewModel：MVI 单向数据流，调度 [TaskRepository] 与 UI 状态/副作用。
 *
 * **调用方式**：UI 通过 [onEvent] 投递 [TaskUiEvent]；订阅 [uiState] 渲染，订阅 [uiEffect] 处理 Toast/导航。
 *
 * **线程约束**：数据操作在 [launchTask] 内执行（仓库层 IO）；本类不直接访问导航 API。
 *
 * @param repository 由 [TaskViewModelFactory] 手动注入
 */
class TaskViewModel(
    private val repository: TaskRepository,
) : BaseViewModel<TaskUiState, TaskUiEvent, TaskUiEffect>(BaseUiState.Loading) {

    private val logTag = "TaskViewModel"

    init {
        loadTasks(isRefresh = false)
    }

    override fun handleEvent(event: TaskUiEvent) {
        when (event) {
            TaskUiEvent.Refresh -> loadTasks(isRefresh = true)
            is TaskUiEvent.AddTask -> addTask(event.title, event.content)
            is TaskUiEvent.TaskItemClicked -> navigateToTaskDetail(event.taskId)
        }
    }

    private fun navigateToTaskDetail(taskId: String) {
        if (taskId.isBlank()) {
            sendEffect(TaskUiEffect.ShowToast("任务标识无效"))
            return
        }
        sendEffect(
            TaskUiEffect.NavigateToEdit(
                url = TaskRoute.detailPath(taskId),
            ),
        )
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
            when (this) {
                is BaseUiState.Success -> {
                    BaseUiState.Success(data.copy(isRefreshing = isRefresh))
                }
                is BaseUiState.Error -> {
                    if (isRefresh) {
                        BaseUiState.Success(TaskListData(isRefreshing = true))
                    } else {
                        BaseUiState.Loading
                    }
                }
                BaseUiState.Empty -> {
                    if (isRefresh) {
                        BaseUiState.Success(TaskListData(isRefreshing = true))
                    } else {
                        BaseUiState.Loading
                    }
                }
                BaseUiState.Loading -> this
            }
        }
    }

    private fun applyLoadSuccess(tasks: List<Task>) {
        val wasRefreshing = when (val state = currentState) {
            is BaseUiState.Success -> state.data.isRefreshing
            else -> false
        }
        if (tasks.isEmpty()) {
            setState { BaseUiState.Empty }
        } else {
            setState {
                BaseUiState.Success(
                    TaskListData(
                        tasks = tasks,
                        isRefreshing = false,
                    ),
                )
            }
        }
        if (wasRefreshing && tasks.isNotEmpty()) {
            sendEffect(TaskUiEffect.ShowToast("刷新成功"))
        }
    }

    private fun applyLoadError(throwable: Throwable) {
        val message = throwable.message ?: "加载任务失败"
        val hasTasks = currentState.getDataOrNull()?.tasks?.isNotEmpty() == true
        if (hasTasks) {
            setState {
                val data = (this as BaseUiState.Success).data
                BaseUiState.Success(data.copy(isRefreshing = false))
            }
        } else {
            setState { BaseUiState.Error(message) }
        }
        sendEffect(TaskUiEffect.ShowToast(message))
    }
}

/**
 * [TaskViewModel] 手动注入工厂（无 Hilt）。
 */
class TaskViewModelFactory(
    private val repository: TaskRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("未知 ViewModel: ${modelClass.name}")
    }
}
