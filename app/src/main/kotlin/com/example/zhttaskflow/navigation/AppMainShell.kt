package com.example.zhttaskflow.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import com.example.zhttaskflow.analytics.ReleaseTaskFlowAnalytics
import com.example.zhttaskflow.base.analytics.TaskFlowAnalytics
import com.example.zhttaskflow.base.analytics.TaskFlowDebugAnalytics
import com.example.zhttaskflow.core.util.isTaskFlowDebugLoggingEnabled
import com.example.zhttaskflow.base.performance.TaskFlowDebugPerformanceReporter
import com.example.zhttaskflow.base.performance.TaskFlowPerformanceReporter
import com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold
import com.example.zhttaskflow.nav.TaskFlowNavHost
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.interceptor.LocalTaskFlowDeepLinkRouteMapper
import com.example.zhttaskflow.nav.interceptor.LocalTaskFlowLoginSession
import com.example.zhttaskflow.nav.interceptor.TaskFlowDeepLinkRouteMapper
import com.example.zhttaskflow.nav.interceptor.TaskFlowLoginSession
import com.example.zhttaskflow.nav.interceptor.rememberTaskFlowAppRouterInterceptorChain
import com.example.zhttaskflow.nav.interceptor.rememberTaskFlowDeepLinkRouteMapper
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry

/**
 * 应用主界面骨架：底部 Tab + NavHost；全局 Snackbar / Loading / 弹窗由 [TaskFlowBaseScaffold] 托管，
 * 路由拦截链与导航宿主同层装配，保障拦截过程 UI 与全站交互规范一致。
 *
 * ## 壳层可替换能力（无 Hilt，CompositionLocal）
 *
 * | 参数 | 默认 | 传递方式 |
 * |------|------|----------|
 * | [analyticsImpl] | Debug：[TaskFlowDebugAnalytics]；Release：[ReleaseTaskFlowAnalytics] | [TaskFlowBaseScaffold] → [LocalTaskFlowAnalytics] |
 * | [performanceImpl] | [TaskFlowDebugPerformanceReporter] | [TaskFlowBaseScaffold] → [LocalTaskFlowPerformance] |
 * | [loginSessionImpl] | 内存 [TaskFlowLoginSession] | [LocalTaskFlowLoginSession] → 拦截链 |
 * | [deepLinkMapperImpl] | [TaskFlowDeepLinkRouteMapperImpl] 样板规则 | [LocalTaskFlowDeepLinkRouteMapper] → 拦截链 |
 *
 * 不传参时行为与改造前一致。
 *
 * ## 登录拦截示范链路
 *
 * - 拦截链：[rememberTaskFlowAppRouterInterceptorChain]（含 [com.example.zhttaskflow.nav.interceptor.TaskFlowLoginInterceptor]）。
 * - 业务标记：任务详情在 [com.example.zhttaskflow.feature.task.navigation.TaskListRouteHost] 对 path 调用
 *   [com.example.zhttaskflow.nav.interceptor.TaskFlowRouteAuthMarker.withNeedLogin]（封装见 [com.example.zhttaskflow.feature.task.navigation.navigationPathRequireLogin]）。
 * - 未登录 → 全局 Dialog 引导 → 模拟登录 → Snackbar 成功 → 自动进入详情；未标记路由正常放行。
 */
@Composable
fun AppMainShell(
    registry: TaskFlowRouteRegistry,
    startDestination: String,
    navigator: TaskFlowNavigator,
    modifier: Modifier = Modifier,
    analyticsImpl: TaskFlowAnalytics? = null,
    performanceImpl: TaskFlowPerformanceReporter? = null,
    loginSessionImpl: TaskFlowLoginSession? = null,
    deepLinkMapperImpl: TaskFlowDeepLinkRouteMapper? = null,
) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedTab = MainTab.fromRoute(currentRoute)

    val analytics = analyticsImpl ?: rememberAppShellAnalytics()
    val performanceReporter = performanceImpl ?: TaskFlowDebugPerformanceReporter
    val loginSession = loginSessionImpl ?: remember { TaskFlowLoginSession() }
    val deepLinkMapper = deepLinkMapperImpl ?: rememberTaskFlowDeepLinkRouteMapper()

    CompositionLocalProvider(
        LocalTaskFlowLoginSession provides loginSession,
        LocalTaskFlowDeepLinkRouteMapper provides deepLinkMapper,
    ) {
        val routerInterceptorChain = rememberTaskFlowAppRouterInterceptorChain()

        TaskFlowBaseScaffold(
            modifier = modifier.fillMaxSize(),
            consumeStatusBarsInContent = false,
            analytics = analytics,
            performanceImpl = performanceReporter,
            bottomBar = {
                selectedTab?.let { tab ->
                    MainBottomNavigationBar(
                        selectedTab = tab,
                        onTabSelected = { selected ->
                            navigator.navigateMainTab(selected.route)
                        },
                    )
                }
            },
        ) { _ ->
            TaskFlowNavHost(
                registry = registry,
                startDestination = startDestination,
                navigator = navigator,
                navController = navController,
                routerInterceptorChain = routerInterceptorChain,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * 应用壳默认埋点：Debug 安装包走调试实现；Release 走 [ReleaseTaskFlowAnalytics]（可经 [analyticsImpl] 覆盖）。
 */
@Composable
private fun rememberAppShellAnalytics(): TaskFlowAnalytics {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        if (isTaskFlowDebugLoggingEnabled()) {
            TaskFlowDebugAnalytics
        } else {
            ReleaseTaskFlowAnalytics
        }
    }
}
