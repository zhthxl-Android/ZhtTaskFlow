package com.example.zhttaskflow.nav

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.example.zhttaskflow.nav.router.RouteRequest
import com.example.zhttaskflow.nav.router.RouterChainOutcome
import com.example.zhttaskflow.nav.router.RouterInterceptorChain
import com.example.zhttaskflow.nav.router.dispatchRouteNavigation
import kotlinx.coroutines.CoroutineScope

/**
 * 导航执行器：业务通过封装方法跳转，不直接持有 [NavHostController]。
 *
 * - [navigate] / [navigateMainTab]：安装非空 [routerInterceptorChain] 时先走拦截链（Loading、Snackbar 错误提示）。
 * - [navigateUp]：弹出返回栈，不经过拦截链。
 * - ViewModel 不应持有本类；跨页跳转优先 `NavigationUiEffect` + RouteHost 消费。
 *
 * @see com.example.zhttaskflow.nav.doc.NavArchitecture
 */
class AppNavigator {

    private var navHostController: NavHostController? = null

    //用于路由拦截器执行异步逻辑（比如登录态校验、弹窗、loading）
    private var interceptScope: CoroutineScope? = null

    //拦截器：跳转前可以插入逻辑：登录校验、权限检查、弹窗、埋点、Snackbar 报错
    private var routerInterceptorChain: RouterInterceptorChain =
        //空拦截链,没安装拦截器时，直接执行跳转，不经过拦截逻辑
        RouterInterceptorChain.Empty

    /** 由 [AppNavHost] 绑定控制器与协程作用域。 */
    fun bind(
        controller: NavHostController,
        interceptScope: CoroutineScope,
    ) {
        navHostController = controller
        this.interceptScope = interceptScope
    }

    /** 装配路由拦截链（通常在壳工程或 NavHost 注入）。 */
    fun installRouterInterceptorChain(chain: RouterInterceptorChain) {
        routerInterceptorChain = chain
    }

    /**
     * 跳转到指定路由。
     *
     * @param route 已注册的路由路径（含参数时传入完整 path）
     */
    fun navigate(route: String) {
        dispatchNavigation(
            request = RouteRequest(targetRoute = route, isMainTab = false),
        ) { outcome ->
            //拦截器全部放行之后回调，执行真实跳转
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
            request = RouteRequest(targetRoute = route, isMainTab = true),
        ) { outcome ->
            performNavigate(route = outcome.route, isMainTab = true)
        }
    }

    /** 返回上一页 */
    fun navigateUp() {
        navHostController?.popBackStack()
    }

    /**
     * 判断是否需要走拦截器
     * */
    private fun dispatchNavigation(
        request: RouteRequest,
        onNavigate: (RouterChainOutcome.Navigate) -> Unit,
    ) {
        val chain = routerInterceptorChain
        val scope = interceptScope
        if (chain.isEmpty || scope == null) {
            onNavigate(
                RouterChainOutcome.Navigate(
                    route = request.targetRoute,
                    isMainTab = request.isMainTab,
                ),
            )
            return
        }
        //启动拦截器链异步执行
        dispatchRouteNavigation(
            scope = scope,
            chain = chain,
            request = request,
            onNavigate = onNavigate,
        )
    }

    /**
     * 执行跳转
     * */
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
            //通知拦截链：路由跳转失败，可以弹出错误提示
            routerInterceptorChain.notifyFailure(message = null)
        }
    }
}
