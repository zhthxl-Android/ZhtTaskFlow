package com.example.zhttaskflow.feature.task.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zhttaskflow.feature.task.data.TaskMockDataSource
import com.example.zhttaskflow.feature.task.data.TaskRepositoryImpl
import com.example.zhttaskflow.feature.task.domain.usecase.AddTaskUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.DeleteTaskUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.GetTaskListUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.UpdateTaskUseCase
import com.example.zhttaskflow.feature.task.presentation.TaskDetailPlaceholderScreen
import com.example.zhttaskflow.feature.task.presentation.TaskListScreen
import com.example.zhttaskflow.feature.task.presentation.TaskViewModel
import com.example.zhttaskflow.feature.task.presentation.TaskViewModelFactory
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowRoute
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry
import com.example.zhttaskflow.nav.route.TaskFlowTaskNavRoutes
import com.example.zhttaskflow.nav.route.simpleRouteEntry
import com.example.zhttaskflow.nav.route.stringArgRouteEntry

/**
 * 任务模块路由注册入口（路由常量统一引用 [TaskFlowTaskNavRoutes]）。
 *
 * 跳转范式：ViewModel 通过 [com.example.zhttaskflow.feature.task.presentation.TaskUiEffect.NavigateToEdit] 下发路由 path，UI 层消费。
 */
sealed interface TaskRoute : TaskFlowRoute {

    data object List : TaskRoute {
        override val route: String = TaskFlowTaskNavRoutes.TASK_LIST
    }

    data class Detail(val taskId: String) : TaskRoute {
        override val route: String = TaskFlowTaskNavRoutes.detailPath(taskId)
    }
}

/**
 * 向 [TaskFlowRouteRegistry] 注册任务列表与详情路由。
 */
fun registerTaskRoutes(
    registry: TaskFlowRouteRegistry,
    @Suppress("UNUSED_PARAMETER") navigator: TaskFlowNavigator,
) {
    registry.register(
        simpleRouteEntry(
            route = TaskFlowTaskNavRoutes.TASK_LIST,
            content = { TaskListRouteHost() },
        ),
    )
    registry.register(
        stringArgRouteEntry(
            route = TaskFlowTaskNavRoutes.TASK_DETAIL,
            argumentName = TaskFlowTaskNavRoutes.ARG_TASK_ID,
            content = { taskId ->
                TaskDetailPlaceholderScreen(taskId = taskId)
            },
        ),
    )
}

@Composable
private fun TaskListRouteHost() {
    // UI 渲染层：仅获取 ViewModel 并挂载列表页，不含业务逻辑
    val factory = rememberTaskViewModelFactory()
    val viewModel: TaskViewModel = viewModel(factory = factory)
    TaskListScreen(viewModel = viewModel)
}

/**
 * 依赖组装层：Mock 仓库 → UseCase → [TaskViewModelFactory]。
 *
 * 不包含业务逻辑；[remember] 缓存 key 与生命周期与原 [TaskListRouteHost] 内联实现一致。
 */
@Composable
private fun rememberTaskViewModelFactory(): TaskViewModelFactory {
    val repository = remember {
        TaskRepositoryImpl(TaskMockDataSource())
    }
    return remember(repository) {
        TaskViewModelFactory(
            getTaskListUseCase = GetTaskListUseCase(repository),
            addTaskUseCase = AddTaskUseCase(repository),
            updateTaskUseCase = UpdateTaskUseCase(repository),
            deleteTaskUseCase = DeleteTaskUseCase(repository),
        )
    }
}
