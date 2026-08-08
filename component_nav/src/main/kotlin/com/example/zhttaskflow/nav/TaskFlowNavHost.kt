package com.example.zhttaskflow.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry

/**
 * 统一 Navigation Compose 宿主：根据 [TaskFlowRouteRegistry] 装配导航图。
 */
@Composable
fun TaskFlowNavHost(
    registry: TaskFlowRouteRegistry,
    startDestination: String,
    navigator: TaskFlowNavigator,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    navigator.bind(navController)
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        registry.entries().forEach { entry ->
            entry.register(this, navController)
        }
    }
}

@Composable
fun rememberTaskFlowNavigator(): TaskFlowNavigator = remember { TaskFlowNavigator() }
