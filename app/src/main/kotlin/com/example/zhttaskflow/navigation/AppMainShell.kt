package com.example.zhttaskflow.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import com.example.zhttaskflow.analytics.ReleaseAnalytics
import com.example.zhttaskflow.exception.ReleaseCrashReporter
import com.example.zhttaskflow.performance.ReleasePerformanceReporter
import com.example.zhttaskflow.base.analytics.Analytics
import com.example.zhttaskflow.base.analytics.DebugAnalytics
import com.example.zhttaskflow.base.exception.CrashReporter
import com.example.zhttaskflow.base.exception.DebugCrashReporter
import com.example.zhttaskflow.core.util.isDebugLoggingEnabled
import com.example.zhttaskflow.base.performance.DebugPerformanceReporter
import com.example.zhttaskflow.base.performance.PerformanceReporter
import com.example.zhttaskflow.base.ui.BaseScaffold
import com.example.zhttaskflow.nav.AppNavHost
import com.example.zhttaskflow.nav.AppNavigator
import com.example.zhttaskflow.nav.interceptor.LocalDeepLinkRouteMapper
import com.example.zhttaskflow.nav.interceptor.LocalLoginSession
import com.example.zhttaskflow.nav.interceptor.DeepLinkRouteMapper
import com.example.zhttaskflow.nav.interceptor.LoginSession
import com.example.zhttaskflow.nav.interceptor.rememberAppRouterInterceptorChain
import com.example.zhttaskflow.nav.interceptor.rememberDeepLinkRouteMapper
import com.example.zhttaskflow.nav.route.RouteRegistry

/**
 * 应用主界面骨架：底部 Tab（资讯 → 任务 → 日志）+ NavHost；全局 Snackbar / Loading / 弹窗由 [BaseScaffold] 托管，
 * 路由拦截链与导航宿主同层装配，保障拦截过程 UI 与全站交互规范一致。
 *
 * ## 壳层可替换能力（无 Hilt，CompositionLocal）
 *
 * | 参数 | 默认 | 传递方式 |
 * |------|------|----------|
 * | [analyticsImpl] | Debug：[DebugAnalytics]；Release：[ReleaseAnalytics] | [BaseScaffold] → [LocalAnalytics] |
 * | [performanceImpl] | Debug：[DebugPerformanceReporter]；Release：[ReleasePerformanceReporter] | [BaseScaffold] → [LocalPerformance] |
 * | [crashReporterImpl] | Debug：[DebugCrashReporter]；Release：[ReleaseCrashReporter] | [BaseScaffold] → [LocalCrashReporter] |
 * | [loginSessionImpl] | 内存 [LoginSession] | [LocalLoginSession] → 拦截链 |
 * | [deepLinkMapperImpl] | [DeepLinkRouteMapperImpl] 样板规则 | [LocalDeepLinkRouteMapper] → 拦截链 |
 *
 * 不传参时行为与改造前一致。
 *
 * ## 登录拦截示范链路
 *
 * - 拦截链：[rememberAppRouterInterceptorChain]（含 [com.example.zhttaskflow.nav.interceptor.LoginInterceptor]）。
 * - 业务标记：任务详情在 [com.example.zhttaskflow.feature.task.navigation.TaskListRouteHost] 对 path 调用
 *   [com.example.zhttaskflow.nav.interceptor.RouteAuthMarker.withNeedLogin]（封装见 [com.example.zhttaskflow.feature.task.navigation.navigationPathRequireLogin]）。
 * - 未登录 → 全局 Dialog 引导 → 模拟登录 → Snackbar 成功 → 自动进入详情；未标记路由正常放行。
 */
@Composable
fun AppMainShell(
    registry: RouteRegistry,
    startDestination: String = MainTab.startDestinationRoute,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    analyticsImpl: Analytics? = null,
    performanceImpl: PerformanceReporter? = null,
    loginSessionImpl: LoginSession? = null,
    deepLinkMapperImpl: DeepLinkRouteMapper? = null,
    crashReporterImpl: CrashReporter? = null,
) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedTab = MainTab.fromRoute(currentRoute)

    val analytics = analyticsImpl ?: rememberAppShellAnalytics()
    val performanceReporter = performanceImpl ?: rememberAppShellPerformanceReporter()
    val loginSession = loginSessionImpl ?: remember { LoginSession() }
    val deepLinkMapper = deepLinkMapperImpl ?: rememberDeepLinkRouteMapper()
    val crashReporter = crashReporterImpl ?: rememberAppShellCrashReporter()

    CompositionLocalProvider(
        LocalLoginSession provides loginSession,
        LocalDeepLinkRouteMapper provides deepLinkMapper,
    ) {
        val routerInterceptorChain = rememberAppRouterInterceptorChain()

        BaseScaffold(
            modifier = modifier.fillMaxSize(),
            consumeStatusBarsInContent = false,
            analytics = analytics,
            performanceImpl = performanceReporter,
            crashReporter = crashReporter,
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
            AppNavHost(
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
 * 应用壳默认 APM：Debug 安装包走调试实现；Release 走 [ReleasePerformanceReporter]（可经 [performanceImpl] 覆盖）。
 */
@Composable
private fun rememberAppShellPerformanceReporter(): PerformanceReporter {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        if (isDebugLoggingEnabled()) {
            DebugPerformanceReporter
        } else {
            ReleasePerformanceReporter
        }
    }
}

/**
 * 应用壳默认崩溃上报：Debug 安装包走调试实现；Release 走 [ReleaseCrashReporter]（可经 [crashReporterImpl] 覆盖）。
 */
@Composable
private fun rememberAppShellCrashReporter(): CrashReporter {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        if (isDebugLoggingEnabled()) {
            DebugCrashReporter
        } else {
            ReleaseCrashReporter
        }
    }
}

/**
 * 应用壳默认埋点：Debug 安装包走调试实现；Release 走 [ReleaseAnalytics]（可经 [analyticsImpl] 覆盖）。
 */
@Composable
private fun rememberAppShellAnalytics(): Analytics {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        if (isDebugLoggingEnabled()) {
            DebugAnalytics
        } else {
            ReleaseAnalytics
        }
    }
}
