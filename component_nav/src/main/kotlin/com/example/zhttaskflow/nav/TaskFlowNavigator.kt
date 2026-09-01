package com.example.zhttaskflow.nav

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.zhttaskflow.nav.router.TaskFlowRouteRequest
import com.example.zhttaskflow.nav.router.TaskFlowRouterChainOutcome
import com.example.zhttaskflow.nav.router.TaskFlowRouterInterceptorChain
import com.example.zhttaskflow.nav.router.dispatchRouteNavigation
import kotlinx.coroutines.CoroutineScope

/**
 * 导航执行器：业务通过封装方法跳转，不直接持有 [NavHostController]。
 *
 * 安装非空 [routerInterceptorChain] 时，[navigate] / [navigateMainTab] 走拦截链（支持 Loading 与失败提示）。
 */
class TaskFlowNavigator {

    private var navHostController: NavHostController? = null
    private var interceptScope: CoroutineScope? = null
    private var routerInterceptorChain: TaskFlowRouterInterceptorChain =
        TaskFlowRouterInterceptorChain.Empty

    /** 由 [TaskFlowNavHost] 绑定控制器与协程作用域。 */
    fun bind(
        controller: NavHostController,
        interceptScope: CoroutineScope,
    ) {
        navHostController = controller
        this.interceptScope = interceptScope
    }

    /** 装配路由拦截链（通常在壳工程或 NavHost 注入）。 */
    fun installRouterInterceptorChain(chain: TaskFlowRouterInterceptorChain) {
        routerInterceptorChain = chain
    }

    /**
     * 跳转到指定路由。
     *
     * @param route 已注册的路由路径（含参数时传入完整 path）
     */
    fun navigate(route: String) {
        dispatchNavigation(
            request = TaskFlowRouteRequest(targetRoute = route, isMainTab = false),
        ) { outcome ->
            performNavigate(route = outcome.route, isMainTab = false)
        }
    }

    /**
     * 主界面底部 Tab 切换：等价于路由跳转，并保留各 Tab 返回栈与页面状态。
     */
    fun navigateMainTab(route: String) {
        val controller = navHostController ?: return
        val currentRoute = controller.currentBackStackEntry?.destination?.route
        if (currentRoute == route) {
            return
        }
        dispatchNavigation(
            request = TaskFlowRouteRequest(targetRoute = route, isMainTab = true),
        ) { outcome ->
            performNavigate(route = outcome.route, isMainTab = true)
        }
    }

    /** 返回上一页 */
    fun navigateUp() {
        navHostController?.popBackStack()
    }

    private fun dispatchNavigation(
        request: TaskFlowRouteRequest,
        onNavigate: (TaskFlowRouterChainOutcome.Navigate) -> Unit,
    ) {
        val chain = routerInterceptorChain
        val scope = interceptScope
        if (chain.isEmpty || scope == null) {
            onNavigate(
                TaskFlowRouterChainOutcome.Navigate(
                    route = request.targetRoute,
                    isMainTab = request.isMainTab,
                ),
            )
            return
        }
        dispatchRouteNavigation(
            scope = scope,
            chain = chain,
            request = request,
            onNavigate = onNavigate,
        )
    }

    private fun performNavigate(route: String, isMainTab: Boolean) {
        val controller = navHostController ?: return
        try {
            if (isMainTab) {
                val startDestinationId = controller.graph.findStartDestination().id
                controller.navigate(route) {
                    popUpTo(startDestinationId) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            } else {
                controller.navigate(route)
            }
        } catch (_: IllegalArgumentException) {
            routerInterceptorChain.notifyFailure(message = null)
        }
    }
}
