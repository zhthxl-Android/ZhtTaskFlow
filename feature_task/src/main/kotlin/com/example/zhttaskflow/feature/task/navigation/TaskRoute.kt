package com.example.zhttaskflow.feature.task.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.zhttaskflow.feature.task.data.TaskMockDataSource
import com.example.zhttaskflow.feature.task.data.TaskRepositoryImpl
import com.example.zhttaskflow.feature.task.ui.TaskDetailPlaceholderScreen
import com.example.zhttaskflow.feature.task.ui.TaskListScreen
import com.example.zhttaskflow.feature.task.ui.TaskViewModel
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowRoute
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry
import com.example.zhttaskflow.nav.route.simpleRouteEntry
import com.example.zhttaskflow.nav.route.stringArgRouteEntry

/**
 * 任务模块路由命名规范：
 * - 前缀 `feature_task/` 与模块名对齐，避免跨 Feature 路由冲突
 * - 列表：`feature_task/list`；详情：`feature_task/detail/{taskId}`，参数名 [ARG_TASK_ID]
 *
 * 跳转范式：在 ViewModel 中调用 [TaskFlowNavigator.navigate]，目标 path 使用 [detailPath] 生成。
 */
sealed interface TaskRoute : TaskFlowRoute {

    data object List : TaskRoute {
        override val route: String = ROUTE_LIST
    }

    data class Detail(val taskId: String) : TaskRoute {
        override val route: String = detailPath(taskId)
    }

    companion object {
        /** 任务列表路由 */
        const val ROUTE_LIST: String = "feature_task/list"

        /** 任务详情路由模板（Navigation 占位符） */
        const val ROUTE_DETAIL: String = "feature_task/detail/{taskId}"

        /** 详情页路径参数名 */
        const val ARG_TASK_ID: String = "taskId"

        /**
         * 生成详情页完整路由 path（用于 [TaskFlowNavigator.navigate]）。
         */
        fun detailPath(taskId: String): String = "feature_task/detail/$taskId"
    }
}

/**
 * 向 [TaskFlowRouteRegistry] 注册任务列表与详情路由。
 *
 * 业务层仅调用本方法注册页面，跳转由 [TaskViewModel] + [TaskFlowNavigator] 完成。
 */
fun registerTaskRoutes(
    registry: TaskFlowRouteRegistry,
    navigator: TaskFlowNavigator,
) {
    registry.register(
        simpleRouteEntry(
            route = TaskRoute.ROUTE_LIST,
            content = {
                TaskListRouteHost(navigator = navigator)
            },
        ),
    )
    registry.register(
        stringArgRouteEntry(
            route = TaskRoute.ROUTE_DETAIL,
            argumentName = TaskRoute.ARG_TASK_ID,
            content = { taskId ->
                TaskDetailPlaceholderScreen(taskId = taskId)
            },
        ),
    )
}

@Composable
private fun TaskListRouteHost(
    navigator: TaskFlowNavigator,
) {
    val viewModel = remember(navigator) {
        TaskViewModel(
            repository = TaskRepositoryImpl(TaskMockDataSource()),
            navigator = navigator,
        )
    }
    TaskListScreen(viewModel = viewModel)
}
