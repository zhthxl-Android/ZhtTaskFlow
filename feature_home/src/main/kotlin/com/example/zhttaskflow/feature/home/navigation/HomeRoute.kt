package com.example.zhttaskflow.feature.home.navigation

import com.example.zhttaskflow.feature.home.presentation.HomeRoute
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry
import com.example.zhttaskflow.nav.route.simpleRouteEntry

/**
 * 首页模块 **路由层**（navigation）：模块对外唯一注册入口。
 *
 * 模块定位：首页入口聚合模块，承载首页业务与功能入口分发。
 * 宿主在 NavHost 中调用 [registerHomeRoutes] 完成路由装配；入口跳转目标由路由字符串注入，避免跨 Feature 直接依赖。
 */

/**
 * 在 [TaskFlowRouteRegistry] 中注册首页路由（与 Feature `registerXxxRoutes` 范式一致）。
 *
 * @param taskListRoute 任务列表路由 path；独立调试或未装配任务模块时可传 null，入口点击不跳转
 * @param articleListRoute 资讯列表路由 path；同上
 * @param onHomeBackPress 首页系统返回行为；宿主可传入 `activity.finish()` 等
 */
fun registerHomeRoutes(
    registry: TaskFlowRouteRegistry,
    @Suppress("UNUSED_PARAMETER") navigator: TaskFlowNavigator,
    taskListRoute: String? = null,
    articleListRoute: String? = null,
    onHomeBackPress: () -> Unit = {},
) {
    registry.register(
        simpleRouteEntry(
            route = TaskFlowNavRoutes.HOME_ROUTE,
            content = {
                HomeRoute(
                    onHomeBackPress = onHomeBackPress,
                    taskListRoute = taskListRoute,
                    articleListRoute = articleListRoute,
                )
            },
        ),
    )
}
