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
import com.example.zhttaskflow.base.analytics.TaskFlowAnalytics
import com.example.zhttaskflow.base.analytics.rememberTaskFlowDebugAnalytics
import com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold
import com.example.zhttaskflow.nav.R
import com.example.zhttaskflow.nav.TaskFlowNavHost
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.deeplink.TaskFlowDeepLinkNavigation
import com.example.zhttaskflow.nav.interceptor.LocalTaskFlowDeepLinkRouteMapper
import com.example.zhttaskflow.nav.interceptor.LocalTaskFlowLoginSession
import com.example.zhttaskflow.nav.interceptor.TaskFlowDeepLinkRouteMapper
import com.example.zhttaskflow.nav.interceptor.TaskFlowLoginSession
import com.example.zhttaskflow.nav.interceptor.rememberTaskFlowAppRouterInterceptorChain
import com.example.zhttaskflow.nav.interceptor.rememberTaskFlowDeepLinkRouteMapper
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry
import com.example.zhttaskflow.nav.router.TaskFlowRouterInterceptorChain

/**
 * 独立调试 Activity 与正式壳一致的 Edge-to-Edge 入口，须在 [androidx.activity.compose.setContent] 之前调用。
 */
fun ComponentActivity.prepareTaskFlowFeatureDebug() {
    enableEdgeToEdge()
}

/**
 * 独立调试 Activity 深链待消费队列（与集成壳 [com.example.zhttaskflow.MainActivity] 相同语义）。
 *
 * 在 [androidx.activity.ComponentActivity.onCreate] / [androidx.activity.ComponentActivity.onNewIntent] 中调用
 * [updateFromIntent]，并传入 [TaskFlowFeatureDebugShell] 的 `deepLinkState` 参数。
 */
@Stable
class TaskFlowFeatureDebugDeepLinkState {
    var pendingUri by mutableStateOf<String?>(null)
        private set

    fun updateFromIntent(intent: Intent?) {
        val uri = TaskFlowDeepLinkNavigation.extractDeepLinkUri(intent) ?: return
        pendingUri = uri
    }

    fun markConsumed() {
        pendingUri = null
    }
}

@Composable
fun rememberTaskFlowFeatureDebugDeepLinkState(): TaskFlowFeatureDebugDeepLinkState {
    return remember { TaskFlowFeatureDebugDeepLinkState() }
}

/**
 * Feature 独立调试壳层：对齐集成宿主 [com.example.zhttaskflow.navigation.AppMainShell] 的全局宿主、
 * 默认拦截链、壳层 CompositionLocal 注入与深链分发，使 standalone 与集成环境行为一致。
 *
 * ## 深链（与 [com.example.zhttaskflow.MainActivity] 对齐）
 *
 * 1. Activity 侧：`val deepLinkState = rememberTaskFlowFeatureDebugDeepLinkState()`，
 *    `onCreate`/`onNewIntent` 中 `deepLinkState.updateFromIntent(intent)`。
 * 2. Manifest 为调试 Activity 声明 `taskflow` `VIEW`（`singleTop` 建议与集成壳一致）。
 * 3. 壳内 [TaskFlowFeatureDebugDeepLinkEffect] 在 NavHost 装配后 `navigate` + 完整拦截链。
 *
 * ## 拦截链
 *
 * @param routerInterceptorChain 默认 `null` 时使用 [rememberTaskFlowAppRouterInterceptorChain]（深链 / 权限 / 登录，与 App 一致）。
 * 传入 [TaskFlowRouterInterceptorChain.Empty] 可关闭拦截。
 *
 * @param mainTabRootRoute 一级 Tab 根路由；当前 destination 与之相等时展示底部导航占位（详情等子页自动隐藏）。
 */
@Composable
fun TaskFlowFeatureDebugShell(
    registry: TaskFlowRouteRegistry,
    startDestination: String,
    navigator: TaskFlowNavigator,
    mainTabRootRoute: String?,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    routerInterceptorChain: TaskFlowRouterInterceptorChain? = null,
    deepLinkState: TaskFlowFeatureDebugDeepLinkState? = null,
    analyticsImpl: TaskFlowAnalytics? = null,
    loginSessionImpl: TaskFlowLoginSession? = null,
    deepLinkMapperImpl: TaskFlowDeepLinkRouteMapper? = null,
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val showMainTabBottomBar = mainTabRootRoute != null && currentRoute == mainTabRootRoute

    val analytics = analyticsImpl ?: rememberTaskFlowDebugAnalytics()
    val loginSession = loginSessionImpl ?: remember { TaskFlowLoginSession() }
    val deepLinkMapper = deepLinkMapperImpl ?: rememberTaskFlowDeepLinkRouteMapper()

    CompositionLocalProvider(
        LocalTaskFlowLoginSession provides loginSession,
        LocalTaskFlowDeepLinkRouteMapper provides deepLinkMapper,
    ) {
        val defaultInterceptorChain = rememberTaskFlowAppRouterInterceptorChain()
        val resolvedInterceptorChain = routerInterceptorChain ?: defaultInterceptorChain

        TaskFlowBaseScaffold(
            modifier = modifier.fillMaxSize(),
            consumeStatusBarsInContent = false,
            analytics = analytics,
            bottomBar = {
                if (showMainTabBottomBar) {
                    TaskFlowStandaloneMainTabBottomBarPlaceholder()
                }
            },
        ) { _ ->
            TaskFlowNavHost(
                registry = registry,
                startDestination = startDestination,
                navigator = navigator,
                navController = navController,
                routerInterceptorChain = resolvedInterceptorChain,
                modifier = Modifier.fillMaxSize(),
            )
            deepLinkState?.let { state ->
                TaskFlowFeatureDebugDeepLinkEffect(
                    navigator = navigator,
                    deepLinkState = state,
                )
            }
        }
    }
}

/**
 * NavHost / 拦截链就绪后消费 [TaskFlowFeatureDebugDeepLinkState.pendingUri]，走与集成壳相同的深链导航。
 */
@Composable
private fun TaskFlowFeatureDebugDeepLinkEffect(
    navigator: TaskFlowNavigator,
    deepLinkState: TaskFlowFeatureDebugDeepLinkState,
) {
    val pendingUri = deepLinkState.pendingUri
    LaunchedEffect(pendingUri) {
        val uri = pendingUri ?: return@LaunchedEffect
        val route = TaskFlowDeepLinkNavigation.prepareNavigationRoute(uri)
        navigator.navigate(route)
        deepLinkState.markConsumed()
    }
}

/**
 * 与宿主 [NavigationBar] 同高度、同 windowInsets，仅作布局占位，不可点击切换。
 */
@Composable
private fun TaskFlowStandaloneMainTabBottomBarPlaceholder() {
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
