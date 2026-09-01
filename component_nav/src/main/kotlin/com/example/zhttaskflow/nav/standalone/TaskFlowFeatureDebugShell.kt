package com.example.zhttaskflow.nav.standalone

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold
import com.example.zhttaskflow.nav.R
import com.example.zhttaskflow.nav.TaskFlowNavHost
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.interceptor.rememberTaskFlowAppRouterInterceptorChain
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry
import com.example.zhttaskflow.nav.router.TaskFlowRouterInterceptorChain

/**
 * 独立调试 Activity 与正式壳一致的 Edge-to-Edge 入口，须在 [androidx.activity.compose.setContent] 之前调用。
 */
fun ComponentActivity.prepareTaskFlowFeatureDebug() {
    enableEdgeToEdge()
}

/**
 * Feature 独立调试壳层：对齐集成宿主 [com.example.zhttaskflow.navigation.AppMainShell] 的全局宿主、
 * 路由拦截链与底部 Tab 占位，使 standalone 与集成环境在 Snackbar / Loading / 登录拦截等行为上一致。
 *
 * @param mainTabRootRoute 一级 Tab 根路由；当前 destination 与之相等时展示底部导航占位（详情等子页自动隐藏）。
 * @param routerInterceptorChain 路由拦截链；默认 `null` 时使用 [rememberTaskFlowAppRouterInterceptorChain]（与 App 壳一致）。
 * 传入 [TaskFlowRouterInterceptorChain.Empty] 可关闭拦截，便于特殊调试场景。
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
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val showMainTabBottomBar = mainTabRootRoute != null && currentRoute == mainTabRootRoute
    val defaultInterceptorChain = rememberTaskFlowAppRouterInterceptorChain()
    val resolvedInterceptorChain = routerInterceptorChain ?: defaultInterceptorChain

    TaskFlowBaseScaffold(
        modifier = modifier.fillMaxSize(),
        consumeStatusBarsInContent = false,
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
