package com.example.zhttaskflow.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.zhttaskflow.feature.home.presentation.HomeRoute
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowRouteEntry
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry

/**
 * 在 [TaskFlowRouteRegistry] 中注册首页路由（与 `registerTaskRoutes` / `registerArticleRoutes` 范式一致）。
 *
 * 模块对外唯一入口；路由常量统一使用 [TaskFlowNavRoutes.HOME_ROUTE]。
 *
 * @param taskListRoute 任务列表路由 path；standalone 或未装配任务模块时可传 null
 * @param articleListRoute 资讯列表路由 path；同上
 * @param onHomeBackPress 首页系统返回；standalone 可传 `activity.finish()`
 */
fun registerHomeRoutes(
    registry: TaskFlowRouteRegistry,
    @Suppress("UNUSED_PARAMETER") navigator: TaskFlowNavigator,
    taskListRoute: String? = null,
    articleListRoute: String? = null,
    onHomeBackPress: () -> Unit = {},
) {
    registry.register(
        TaskFlowRouteEntry(
            route = TaskFlowNavRoutes.HOME_ROUTE,
            register = {
                registerHomeRoutes(
                    taskListRoute = taskListRoute,
                    articleListRoute = articleListRoute,
                    onHomeBackPress = onHomeBackPress,
                )
            },
        ),
    )
}

/**
 * 在 NavGraph 上注册首页 composable（由 [registerHomeRoutes] 经 [TaskFlowRouteEntry] 调用，不对外暴露）。
 */
internal fun NavGraphBuilder.registerHomeRoutes(
    taskListRoute: String? = null,
    articleListRoute: String? = null,
    onHomeBackPress: () -> Unit = {},
) {
    composable(route = TaskFlowNavRoutes.HOME_ROUTE) {
        HomeRoute(
            onHomeBackPress = onHomeBackPress,
            taskListRoute = taskListRoute,
            articleListRoute = articleListRoute,
        )
    }
}
