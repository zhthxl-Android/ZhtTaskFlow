package com.example.zhttaskflow.feature.task.ui

import com.example.zhttaskflow.base.foundation.TaskFlowIllegalStateException
import com.example.zhttaskflow.base.foundation.TaskFlowLogger
import com.example.zhttaskflow.base.mvi.BaseViewModel
import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskRepository
import com.example.zhttaskflow.feature.task.domain.TaskStatus
import com.example.zhttaskflow.feature.task.navigation.TaskRoute
import com.example.zhttaskflow.nav.TaskFlowNavigator

/**
 * 任务列表 ViewModel：事件 → 仓库 / 导航 → 状态/副作用，严格 MVI 单向数据流。
 *
 * 路由跳转通过 [TaskFlowNavigator] 完成，不直接使用 Navigation Compose API。
 *
 * @param repository 由外部手动构造注入
 * @param navigator 与 [com.example.zhttaskflow.nav.TaskFlowNavHost] 绑定的导航器
 */
class TaskViewModel(
    private val repository: TaskRepository,
    private val navigator: TaskFlowNavigator,
) : BaseViewModel<TaskUiState, TaskUiEvent, TaskUiEffect>(TaskUiState()) {

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

    /**
     * 跳转详情：路径规范见 [TaskRoute.detailPath]。
     */
    private fun navigateToTaskDetail(taskId: String) {
        if (taskId.isBlank()) {
            sendEffect(TaskUiEffect.ShowToast("任务标识无效"))
            return
        }
        launchTask(
            tag = logTag,
            onError = { throwable ->
                TaskFlowLogger.e(logTag, "跳转任务详情失败", throwable)
                sendEffect(
                    TaskUiEffect.ShowToast(
                        throwable.message ?: "无法打开任务详情",
                    ),
                )
            },
        ) {
            val route = TaskRoute.detailPath(taskId)
            try {
                navigator.navigate(route)
            } catch (throwable: Throwable) {
                throw TaskFlowIllegalStateException(
                    message = "路由跳转异常: $route",
                    cause = throwable,
                )
            }
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
        val wasRefreshing = currentState.isRefreshing
        setState {
            copy(
                tasks = tasks,
                isLoading = false,
                isRefreshing = false,
                errorMessage = null,
            )
        }
        if (wasRefreshing) {
            sendEffect(TaskUiEffect.ShowToast("刷新成功"))
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
