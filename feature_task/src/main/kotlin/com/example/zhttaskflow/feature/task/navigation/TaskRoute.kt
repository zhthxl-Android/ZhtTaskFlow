package com.example.zhttaskflow.feature.task.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zhttaskflow.feature.task.data.TaskMockDataSource
import com.example.zhttaskflow.feature.task.data.TaskRepositoryImpl
import com.example.zhttaskflow.feature.task.domain.usecase.AddTaskUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.DeleteTaskUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.GetTaskByIdUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.GetTaskListUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.ObserveTaskDataChangesUseCase
import com.example.zhttaskflow.feature.task.domain.usecase.UpdateTaskUseCase
import com.example.zhttaskflow.feature.task.presentation.TaskDetailPlaceholderScreen
import com.example.zhttaskflow.feature.task.presentation.TaskDetailViewModel
import com.example.zhttaskflow.feature.task.presentation.TaskDetailViewModelFactory
import com.example.zhttaskflow.feature.task.presentation.TaskListScreen
import com.example.zhttaskflow.feature.task.presentation.TaskUiEffect
import com.example.zhttaskflow.base.ext.UiEffectConsumption
import com.example.zhttaskflow.feature.task.presentation.TaskViewModel
import com.example.zhttaskflow.feature.task.presentation.TaskViewModelFactory
import com.example.zhttaskflow.nav.LocalNavigator
import com.example.zhttaskflow.nav.AppNavigator
import com.example.zhttaskflow.nav.interceptor.RouteGatePolicy
import com.example.zhttaskflow.nav.route.Route
import com.example.zhttaskflow.nav.route.RouteRegistry
import com.example.zhttaskflow.nav.route.TaskNavRoutes
import com.example.zhttaskflow.nav.route.simpleRouteEntry
import com.example.zhttaskflow.nav.route.stringArgRouteEntry

/**
 * 任务模块路由注册入口（路由常量统一引用 [TaskNavRoutes]）。
 *
 * 跳转范式：ViewModel 通过 [com.example.zhttaskflow.feature.task.presentation.TaskUiEffect.NavigateToEdit] 下发路由 path，路由宿主消费。
 */
sealed interface TaskRoute : Route {

    data object List : TaskRoute {
        override val route: String = TaskNavRoutes.TASK_LIST
    }

    data class Detail(val taskId: String) : TaskRoute {
        override val route: String = TaskNavRoutes.detailPath(taskId)
    }
}

/**
 * **登录 + 权限拦截**：策略见 [RouteGatePolicy] 与 `docs/TASKFLOW_ROUTE_GATES.md`。
 *
 * 1. ViewModel 仍下发「纯净」Navigation path（不含 query），见 [TaskUiEffect.NavigateToEdit]。
 * 2. RouteHost 在 [AppNavigator.navigate] 前调用 [RouteGatePolicy.enrichNavigationPath]。
 * 3. 壳工程已装配 [com.example.zhttaskflow.nav.interceptor.rememberAppRouterInterceptorChain]；
 *    权限先于登录执行；未列入策略表的路由不受影响。
 */
internal fun taskDetailPathRequireLogin(taskId: String): String {
    return RouteGatePolicy.enrichNavigationPath(TaskNavRoutes.detailPath(taskId))
}

/**
 * 任务详情 path：登录 + 存储权限（与 [RouteGatePolicy] 一致）。
 */
internal fun taskDetailPathRequireLoginAndStorage(taskId: String): String {
    return taskDetailPathRequireLogin(taskId)
}

/**
 * RouteHost 侧：对目标 path 施加策略表中的门禁标记。
 */
internal fun navigationPathRequireLogin(targetRoute: String): String {
    return RouteGatePolicy.enrichNavigationPath(targetRoute)
}

/**
 * @deprecated 与 [navigationPathRequireLogin] 等价，保留命名以兼容调用方。
 */
internal fun navigationPathRequireStoragePermission(targetRoute: String): String {
    return RouteGatePolicy.enrichNavigationPath(targetRoute)
}

/**
 * 向 [RouteRegistry] 注册任务列表与详情路由。
 */
fun registerTaskRoutes(
    registry: RouteRegistry,
    /** 与全局路由注册签名对齐，跳转由 Composable 内 [LocalNavigator] 消费。 */
    @Suppress("UNUSED_PARAMETER") navigator: AppNavigator,
) {
    registry.register(
        simpleRouteEntry(
            route = TaskNavRoutes.TASK_LIST,
            content = { TaskListRouteHost() },
        ),
    )
    registry.register(
        stringArgRouteEntry(
            route = TaskNavRoutes.TASK_DETAIL,
            argumentName = TaskNavRoutes.ARG_TASK_ID,
            content = { taskId ->
                TaskDetailRouteHost(
                    taskId = taskId,
                )
            },
        ),
    )
}

/**
 * 任务列表路由宿主：组装 ViewModel 与 [TaskListScreen]。
 *
 * **导航类 Effect 订阅方（Route 层）**：仅处理 [NavigationUiEffect]（[TaskUiEffect.NavigateToEdit]）。
 * 展示类 Effect 由 [TaskListScreen] 消费，见 [UiEffectConsumption]。
 */
@Composable
private fun TaskListRouteHost() {
    val navigator = LocalNavigator.current
    val factory = rememberTaskViewModelFactory()
    val viewModel: TaskViewModel = viewModel(factory = factory)

    // Route 层 Collector：仅处理 [NavigationUiEffect]（[TaskUiEffect.NavigateToEdit]）。
    // 展示类 Effect 由 [TaskListScreen] 消费，见 [UiEffectConsumption]。
    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is TaskUiEffect.NavigateToEdit -> {
                    navigator.navigate(
                        navigationPathRequireLogin(effect.url),
                    )
                }
                else -> {
                    // PresentationUiEffect：由 TaskListScreen 消费
                }
            }
        }
    }

    TaskListScreen(viewModel = viewModel)
}

@Composable
private fun TaskDetailRouteHost(taskId: String) {
    val navigator = LocalNavigator.current
    val factory = rememberTaskDetailViewModelFactory(taskId = taskId)
    val viewModel: TaskDetailViewModel = viewModel(factory = factory)
    TaskDetailPlaceholderScreen(
        viewModel = viewModel,
        taskId = taskId,
        onNavigateUp = { navigator.navigateUp() },
    )
}

@Composable
private fun rememberTaskRepository(): TaskRepositoryImpl {
    return remember {
        TaskRepositoryImpl(TaskMockDataSource.shared)
    }
}

@Composable
private fun rememberTaskDetailViewModelFactory(taskId: String): TaskDetailViewModelFactory {
    val repository = rememberTaskRepository()
    return remember(repository, taskId) {
        TaskDetailViewModelFactory(
            taskId = taskId,
            getTaskByIdUseCase = GetTaskByIdUseCase(repository),
            updateTaskUseCase = UpdateTaskUseCase(repository),
        )
    }
}

/**
 * 依赖组装层：Mock 仓库 → UseCase → [TaskViewModelFactory]。
 *
 * 不包含业务逻辑；[remember] 缓存 key 与生命周期与原 [TaskListRouteHost] 内联实现一致。
 */
@Composable
private fun rememberTaskViewModelFactory(): TaskViewModelFactory {
    val repository = rememberTaskRepository()
    return remember(repository) {
        TaskViewModelFactory(
            getTaskListUseCase = GetTaskListUseCase(repository),
            observeTaskDataChangesUseCase = ObserveTaskDataChangesUseCase(repository),
            addTaskUseCase = AddTaskUseCase(repository),
            updateTaskUseCase = UpdateTaskUseCase(repository),
            deleteTaskUseCase = DeleteTaskUseCase(repository),
        )
    }
}
