package com.example.zhttaskflow.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.zhttaskflow.base.ui.TaskFlowInsetsPolicy
import com.example.zhttaskflow.base.ui.rememberTaskFlowScaffoldContentPadding
import com.example.zhttaskflow.nav.TaskFlowNavHost
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry

/**
 * 应用主界面骨架：底部 Tab + NavHost；inset 策略与 [TaskFlowInsetsPolicy] 对齐，不向子页面传递状态栏 top。
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = TaskFlowInsetsPolicy.scaffoldContentWindowInsets,
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
    ) { innerPadding ->
        val shellContentPadding = rememberTaskFlowScaffoldContentPadding(scaffoldPadding = innerPadding)
        TaskFlowNavHost(
            registry = registry,
            startDestination = startDestination,
            navigator = navigator,
            navController = navController,
            modifier = Modifier
                .fillMaxSize()
                .padding(shellContentPadding),
            enterTransition = { mainTabEnterTransition() },
            exitTransition = { mainTabExitTransition() },
            popEnterTransition = { mainTabEnterTransition() },
            popExitTransition = { mainTabExitTransition() },
        )
    }
}
