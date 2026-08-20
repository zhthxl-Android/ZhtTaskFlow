package com.example.zhttaskflow.navigation

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.zhttaskflow.feature.article.navigation.registerArticleRoutes
import com.example.zhttaskflow.feature.task.navigation.TaskRoute
import com.example.zhttaskflow.feature.task.navigation.registerTaskRoutes
import com.example.zhttaskflow.nav.LocalTaskFlowNavigator
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowArticleNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowRouteEntry
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry
import com.example.zhttaskflow.ui.home.HomeScreen

/**
 * 首页路由宿主：组装 [HomeScreen] 与 [TaskFlowNavigator] 跳转，不承载业务逻辑。
 */
@Composable
internal fun HomeRoute() {
    val navigator = LocalTaskFlowNavigator.current
    val activity = LocalContext.current as ComponentActivity
    BackHandler {
        activity.finish()
    }
    HomeScreen(
        onTaskClick = { navigator.navigate(TaskRoute.ROUTE_LIST) },
        onArticleClick = { navigator.navigate(TaskFlowArticleNavRoutes.LIST) },
    )
}

/**
 * 在 [TaskFlowRouteRegistry] 中注册首页路由（与 Feature `registerXxxRoutes` 范式一致）。
 */
internal fun TaskFlowRouteRegistry.registerHomeRoute() {
    register(
        TaskFlowRouteEntry(
            route = TaskFlowNavRoutes.HOME_ROUTE,
            register = { registerHomeRoute() },
        ),
    )
}

/**
 * 在 NavGraph 上注册首页 composable 节点。
 */
internal fun NavGraphBuilder.registerHomeRoute() {
    composable(route = TaskFlowNavRoutes.HOME_ROUTE) {
        HomeRoute()
    }
}

/**
 * 注册任务与资讯业务路由（保持各 Feature 原有注册实现不变）。
 */
internal fun TaskFlowRouteRegistry.registerAppFeatureRoutes(navigator: TaskFlowNavigator) {
    registerTaskRoutes(registry = this, navigator = navigator)
    registerArticleRoutes(registry = this, navigator = navigator)
}
