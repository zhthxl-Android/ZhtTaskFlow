package com.example.zhttaskflow.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold
import com.example.zhttaskflow.nav.TaskFlowNavHost
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.interceptor.rememberTaskFlowAppRouterInterceptorChain
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry

/**
 * 应用主界面骨架：底部 Tab + NavHost；全局 Snackbar / Loading / 弹窗由 [TaskFlowBaseScaffold] 托管，
 * 路由拦截链与导航宿主同层装配，保障拦截过程 UI 与全站交互规范一致。
 */
@Composable
fun AppMainShell(
    registry: TaskFlowRouteRegistry,
    startDestination: String,
    navigator: TaskFlowNavigator,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val selectedTab = MainTab.fromRoute(currentRoute)
    val routerInterceptorChain = rememberTaskFlowAppRouterInterceptorChain()

    TaskFlowBaseScaffold(
        modifier = modifier.fillMaxSize(),
        consumeStatusBarsInContent = false,
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
