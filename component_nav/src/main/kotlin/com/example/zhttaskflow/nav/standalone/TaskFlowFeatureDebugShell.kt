package com.example.zhttaskflow.nav.standalone

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.zhttaskflow.base.ui.TaskFlowInsetsPolicy
import com.example.zhttaskflow.base.ui.rememberTaskFlowScaffoldContentPadding
import com.example.zhttaskflow.nav.R
import com.example.zhttaskflow.nav.TaskFlowNavHost
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry

/**
 * 独立调试 Activity 与正式壳一致的 Edge-to-Edge 入口，须在 [androidx.activity.compose.setContent] 之前调用。
 */
fun ComponentActivity.prepareTaskFlowFeatureDebug() {
    enableEdgeToEdge()
}

/**
 * Feature 独立调试壳层：对齐宿主 [com.example.zhttaskflow.navigation.AppMainShell] 的 inset 与底部 Tab 占位，
 * 使业务页面在 standalone 与集成宿主中布局一致。
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
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val showMainTabBottomBar = mainTabRootRoute != null && currentRoute == mainTabRootRoute

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = TaskFlowInsetsPolicy.scaffoldContentWindowInsets,
        bottomBar = {
            if (showMainTabBottomBar) {
                TaskFlowStandaloneMainTabBottomBarPlaceholder()
            }
        },
    ) { innerPadding ->
        val shellContentPadding = rememberTaskFlowScaffoldContentPadding(scaffoldPadding = innerPadding)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(shellContentPadding),
        ) {
            TaskFlowNavHost(
                registry = registry,
                startDestination = startDestination,
                navigator = navigator,
                navController = navController,
                modifier = Modifier.fillMaxSize(),
            )
        }
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
