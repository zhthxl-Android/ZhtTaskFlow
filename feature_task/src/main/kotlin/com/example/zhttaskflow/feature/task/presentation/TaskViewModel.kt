package com.example.zhttaskflow.feature.task.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.mvi.BaseViewModel
import com.example.zhttaskflow.base.mvi.getDataOrNull
import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskStatus
import com.example.zhttaskflow.feature.task.domain.usecase.AddTaskUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.DeleteTaskUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.GetTaskListUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.ObserveTaskDataChangesUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.UpdateTaskUseCase
import com.example.zhttaskflow.nav.route.TaskFlowTaskNavRoutes
import kotlinx.coroutines.launch

/**
 * 任务列表 ViewModel：MVI 单向数据流，通过领域用例调度任务数据与 UI 状态/副作用。
 *
 * **调用方式**：UI 通过 [onEvent] 投递 [TaskUiEvent]；订阅 [uiState] 渲染，订阅 [uiEffect] 处理 Snackbar/导航。
 *
 * **线程约束**：数据操作在 [launchTask] 内执行（用例 → 仓库 IO）；本类不直接访问 Repository 与导航 API。
 */
class TaskViewModel(
    private val getTaskListUseCase: GetTaskListUseCase,
    private val observeTaskDataChangesUseCase: ObserveTaskDataChangesUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    /** 预留：任务编辑能力接入后使用，工厂保持完整注入避免后续改签名。 */
    @Suppress("UnusedPrivateProperty")
    private val updateTaskUseCase: UpdateTaskUseCase,
    /** 预留：任务删除能力接入后使用，工厂保持完整注入避免后续改签名。 */
    @Suppress("UnusedPrivateProperty")
    private val deleteTaskUseCase: DeleteTaskUseCase,
) : BaseViewModel<TaskUiState, TaskUiEvent, TaskUiEffect>(BaseUiState.Loading) {

    private val logTag = "TaskViewModel"

    // region 初始化入口

    init {
        loadTasks(isRefresh = false)
        observeTaskDataChanges()
    }

    // endregion

    // region 事件分发

    override fun handleEvent(event: TaskUiEvent) {
        when (event) {
            TaskUiEvent.Refresh -> loadTasks(isRefresh = true)
            is TaskUiEvent.AddTask -> addTask(event.title, event.content)
            is TaskUiEvent.TaskItemClicked -> navigateToTaskDetail(event.taskId)
        }
    }

    // endregion

    // region 列表-详情数据同步

    private fun observeTaskDataChanges() {
        viewModelScope.launch {
            observeTaskDataChangesUseCase().collect {
                syncTasksFromRepository()
            }
        }
    }

    /**
     * 响应仓库变更事件：静默拉取最新列表，不展示下拉刷新指示与「刷新成功」提示。
     */
    private fun syncTasksFromRepository() {
        if (currentState is BaseUiState.Loading) {
            return
        }
        launchTask(
            tag = logTag,
            scene = "syncTasksFromRepository",
            userMessageFallback = "同步任务列表失败",
            onError = { _, message -> applyLoadError(userMessage = message) },
        ) {
            val tasks = getTaskListUseCase()
            applyLoadSuccess(tasks)
        }
    }

    // endregion

    // region 任务列表加载与下拉刷新

    private fun loadTasks(isRefresh: Boolean) {
        launchTask(
            tag = logTag,
            userMessageFallback = "加载任务失败，请稍后重试",
            onError = { _, message -> applyLoadError(userMessage = message) },
        ) {
            applyLoadingState(isRefresh)
            val tasks = getTaskListUseCase()
            applyLoadSuccess(tasks)
        }
    }

    // endregion

    // region 新增任务

    private fun addTask(title: String, content: String) {
        if (title.isBlank()) {
            sendEffect(
                TaskUiEffect.ShowSnackbar(
                    message = "请输入任务标题",
                    type = SnackbarType.Error,
                ),
            )
            return
        }
        launchTask(
            tag = logTag,
            userMessageFallback = "新增任务失败，请稍后重试",
            onError = { _, message ->
                sendEffect(
                    TaskUiEffect.ShowSnackbar(message = message, type = SnackbarType.Error),
                )
            },
        ) {
            val task = Task(
                id = "task-${System.currentTimeMillis()}",
                title = title.trim(),
                content = content.trim(),
                createdAt = System.currentTimeMillis(),
                status = TaskStatus.PENDING,
            )
            addTaskUseCase(task)
            sendEffect(
                TaskUiEffect.ShowSnackbar(
                    message = "任务已添加",
                    type = SnackbarType.Success,
                ),
            )
        }
    }

    // endregion

    // region 结果处理

    private fun applyLoadingState(isRefresh: Boolean) {
        setState {
            when (this) {
                is BaseUiState.Success -> {
                    BaseUiState.Success(data.withRefreshingFlag(isRefresh))
                }
                is BaseUiState.Error -> {
                    if (isRefresh) {
                        BaseUiState.Success(TaskListData().withRefreshing())
                    } else {
                        BaseUiState.Loading
                    }
                }
                BaseUiState.Empty -> {
                    if (isRefresh) {
                        BaseUiState.Success(TaskListData().withRefreshing())
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
            sendEffect(
                TaskUiEffect.ShowSnackbar(
                    message = "刷新成功",
                    type = SnackbarType.Success,
                ),
            )
        }
    }

    private fun applyLoadError(userMessage: String) {
        val hasTasks = currentState.getDataOrNull()?.tasks?.isNotEmpty() == true
        if (hasTasks) {
            setState {
                val data = (this as BaseUiState.Success).data
                BaseUiState.Success(data.withRefreshEnded())
            }
        } else {
            setState { BaseUiState.Error(userMessage) }
        }
        sendEffect(
            TaskUiEffect.ShowSnackbar(message = userMessage, type = SnackbarType.Error),
        )
    }

    // endregion

    // region 导航处理

    private fun navigateToTaskDetail(taskId: String) {
        if (taskId.isBlank()) {
            sendEffect(
                TaskUiEffect.ShowSnackbar(
                    message = "任务标识无效",
                    type = SnackbarType.Error,
                ),
            )
            return
        }
        sendEffect(
            TaskUiEffect.NavigateToEdit(
                url = TaskFlowTaskNavRoutes.detailPath(taskId),
            ),
        )
    }

    // endregion
}

private fun TaskListData.withRefreshing(): TaskListData = copy(isRefreshing = true)

private fun TaskListData.withRefreshingFlag(isRefreshing: Boolean): TaskListData =
    copy(isRefreshing = isRefreshing)

private fun TaskListData.withRefreshEnded(): TaskListData = copy(isRefreshing = false)

/**
 * [TaskViewModel] 手动注入工厂（无 Hilt）。
 */
class TaskViewModelFactory(
    private val getTaskListUseCase: GetTaskListUseCase,
    private val observeTaskDataChangesUseCase: ObserveTaskDataChangesUseCase,
    private val addTaskUseCase: AddTaskUseCase,
    private val updateTaskUseCase: UpdateTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(
                getTaskListUseCase = getTaskListUseCase,
                observeTaskDataChangesUseCase = observeTaskDataChangesUseCase,
                addTaskUseCase = addTaskUseCase,
                updateTaskUseCase = updateTaskUseCase,
                deleteTaskUseCase = deleteTaskUseCase,
            ) as T
        }
        throw IllegalArgumentException("未知 ViewModel: ${modelClass.name}")
    }
}
