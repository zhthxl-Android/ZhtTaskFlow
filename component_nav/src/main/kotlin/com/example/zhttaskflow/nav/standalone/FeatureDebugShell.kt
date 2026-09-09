package com.example.zhttaskflow.nav.standalone

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.zhttaskflow.base.analytics.Analytics
import com.example.zhttaskflow.base.analytics.rememberDebugAnalytics
import com.example.zhttaskflow.base.exception.CrashReporter
import com.example.zhttaskflow.base.exception.DebugCrashReporter
import com.example.zhttaskflow.base.exception.rememberCrashReporter
import com.example.zhttaskflow.base.performance.DebugPerformanceReporter
import com.example.zhttaskflow.base.performance.PerformanceReporter
import com.example.zhttaskflow.base.ui.BaseScaffold
import com.example.zhttaskflow.nav.R
import com.example.zhttaskflow.nav.AppNavHost
import com.example.zhttaskflow.nav.AppNavigator
import com.example.zhttaskflow.nav.deeplink.DeepLinkNavigation
import com.example.zhttaskflow.nav.interceptor.LocalDeepLinkRouteMapper
import com.example.zhttaskflow.nav.interceptor.LocalLoginSession
import com.example.zhttaskflow.nav.interceptor.DeepLinkRouteMapper
import com.example.zhttaskflow.nav.interceptor.LoginSession
import com.example.zhttaskflow.nav.interceptor.rememberAppRouterInterceptorChain
import com.example.zhttaskflow.nav.interceptor.rememberDeepLinkRouteMapper
import com.example.zhttaskflow.nav.route.RouteRegistry
import com.example.zhttaskflow.nav.router.RouterInterceptorChain

/**
 * 独立调试 Activity 与正式壳一致的 Edge-to-Edge 入口，须在 [androidx.activity.compose.setContent] 之前调用。
 */
fun ComponentActivity.prepareFeatureDebug() {
    enableEdgeToEdge()
}

/**
 * 独立调试 Activity 深链待消费队列（与集成壳 [com.example.zhttaskflow.MainActivity] 相同语义）。
 *
 * 在 [androidx.activity.ComponentActivity.onCreate] / [androidx.activity.ComponentActivity.onNewIntent] 中调用
 * [updateFromIntent]，并传入 [FeatureDebugShell] 的 `deepLinkState` 参数。
 */
@Stable
class FeatureDebugDeepLinkState {
    var pendingUri by mutableStateOf<String?>(null)
        private set

    fun updateFromIntent(intent: Intent?) {
        val uri = DeepLinkNavigation.extractDeepLinkUri(intent) ?: return
        pendingUri = uri
    }

    fun markConsumed() {
        pendingUri = null
    }
}

@Composable
fun rememberFeatureDebugDeepLinkState(): FeatureDebugDeepLinkState {
    return remember { FeatureDebugDeepLinkState() }
}

/**
 * Feature 独立调试壳层：对齐集成宿主 [com.example.zhttaskflow.navigation.AppMainShell] 的全局宿主、
 * 默认拦截链、壳层 CompositionLocal 注入与深链分发，使 standalone 与集成环境行为一致。
 *
 * ## 深链（与 [com.example.zhttaskflow.MainActivity] 对齐）
 *
 * 1. Activity 侧：`val deepLinkState = rememberFeatureDebugDeepLinkState()`，
 *    `onCreate`/`onNewIntent` 中 `deepLinkState.updateFromIntent(intent)`。
 * 2. Manifest 为调试 Activity 声明 `taskflow` `VIEW`（`singleTop` 建议与集成壳一致）。
 * 3. 壳内 [FeatureDebugDeepLinkEffect] 在 NavHost 装配后 `navigate` + 完整拦截链。
 *
 * ## 拦截链
 *
 * @param routerInterceptorChain 默认 `null` 时使用 [rememberAppRouterInterceptorChain]（深链 / 权限 / 登录，与 App 一致）。
 * 传入 [RouterInterceptorChain.Empty] 可关闭拦截。
 *
 * ## 壳层可观测注入（与 [com.example.zhttaskflow.navigation.AppMainShell] 对齐）
 *
 * | 参数 | 默认（不传参） | 说明 |
 * |------|----------------|------|
 * | [analyticsImpl] | [rememberDebugAnalytics] | 可注入 `app` 模块 [com.example.zhttaskflow.analytics.ReleaseAnalytics] 模拟 Release |
 * | [performanceImpl] | [DebugPerformanceReporter] | 可注入 [com.example.zhttaskflow.performance.ReleasePerformanceReporter] |
 * | [crashReporterImpl] | [DebugCrashReporter] | 可注入 [com.example.zhttaskflow.exception.ReleaseCrashReporter] |
 * | [loginSessionImpl] | 内存 [LoginSession] | 拦截链登录态 |
 * | [deepLinkMapperImpl] | 样板深链映射 | 拦截链深链解析 |
 *
 * 不传参时行为与改造前完全一致（均为调试版可观测实现）。
 *
 * @param mainTabRootRoute 一级 Tab 根路由；当前 destination 与之相等时展示底部导航占位（详情等子页自动隐藏）。
 */
@Composable
fun FeatureDebugShell(
    registry: RouteRegistry,
    startDestination: String,
    navigator: AppNavigator,
    mainTabRootRoute: String?,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    routerInterceptorChain: RouterInterceptorChain? = null,
    deepLinkState: FeatureDebugDeepLinkState? = null,
    analyticsImpl: Analytics? = null,
    performanceImpl: PerformanceReporter? = null,
    crashReporterImpl: CrashReporter? = null,
    loginSessionImpl: LoginSession? = null,
    deepLinkMapperImpl: DeepLinkRouteMapper? = null,
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val showMainTabBottomBar = mainTabRootRoute != null && currentRoute == mainTabRootRoute

    val analytics = analyticsImpl ?: rememberFeatureDebugShellAnalytics()
    val performanceReporter = performanceImpl ?: rememberFeatureDebugShellPerformanceReporter()
    val crashReporter = crashReporterImpl ?: rememberFeatureDebugShellCrashReporter()
    val loginSession = loginSessionImpl ?: remember { LoginSession() }
    val deepLinkMapper = deepLinkMapperImpl ?: rememberDeepLinkRouteMapper()

    CompositionLocalProvider(
        LocalLoginSession provides loginSession,
        LocalDeepLinkRouteMapper provides deepLinkMapper,
    ) {
        val defaultInterceptorChain = rememberAppRouterInterceptorChain()
        val resolvedInterceptorChain = routerInterceptorChain ?: defaultInterceptorChain

        BaseScaffold(
            modifier = modifier.fillMaxSize(),
            consumeStatusBarsInContent = false,
            analytics = analytics,
            performanceImpl = performanceReporter,
            crashReporter = crashReporter,
            bottomBar = {
                if (showMainTabBottomBar) {
                    StandaloneMainTabBottomBarPlaceholder()
                }
            },
        ) { _ ->
            AppNavHost(
                registry = registry,
                startDestination = startDestination,
                navigator = navigator,
                navController = navController,
                routerInterceptorChain = resolvedInterceptorChain,
                modifier = Modifier.fillMaxSize(),
            )
            deepLinkState?.let { state ->
                FeatureDebugDeepLinkEffect(
                    navigator = navigator,
                    deepLinkState = state,
                )
            }
        }
    }
}

/**
 * 独立调试壳默认埋点（调试实现）；与集成壳 Debug 包行为一致。
 */
@Composable
private fun rememberFeatureDebugShellAnalytics(): Analytics {
    return rememberDebugAnalytics()
}

/**
 * 独立调试壳默认 APM（调试实现）；可通过 [FeatureDebugShell] 的 [performanceImpl] 覆盖为 Release 实现。
 */
@Composable
private fun rememberFeatureDebugShellPerformanceReporter(): PerformanceReporter {
    return remember { DebugPerformanceReporter }
}

/**
 * 独立调试壳默认崩溃上报（调试实现）；可通过 [crashReporterImpl] 覆盖为 Release 实现。
 */
@Composable
private fun rememberFeatureDebugShellCrashReporter(): CrashReporter {
    return rememberCrashReporter(override = DebugCrashReporter)
}

/**
 * NavHost / 拦截链就绪后消费 [FeatureDebugDeepLinkState.pendingUri]，走与集成壳相同的深链导航。
 */
@Composable
private fun FeatureDebugDeepLinkEffect(
    navigator: AppNavigator,
    deepLinkState: FeatureDebugDeepLinkState,
) {
    val pendingUri = deepLinkState.pendingUri
    LaunchedEffect(pendingUri) {
        val uri = pendingUri ?: return@LaunchedEffect
        val route = DeepLinkNavigation.prepareNavigationRoute(uri)
        navigator.navigate(route)
        deepLinkState.markConsumed()
    }
}

/**
 * 与宿主 [NavigationBar] 同高度、同 windowInsets，仅作布局占位，不可点击切换。
 */
@Composable
private fun StandaloneMainTabBottomBarPlaceholder() {
    val label = stringResource(id = R.string.nav_standalone_debug_tab_placeholder)
    NavigationBar(
        windowInsets = NavigationBarDefaults.windowInsets,
    ) {
        NavigationBarItem(
            selected = true,
            onClick = {},
            enabled = false,
            icon = { },
            label = {
                Text(text = label)
            },
            colors = NavigationBarItemDefaults.colors(
                disabledIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}
