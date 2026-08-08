package com.example.zhttaskflow.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.zhttaskflow.nav.route.TaskFlowRoute
import com.example.zhttaskflow.nav.route.TaskFlowRouteArguments
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry

/**
 * Compose 导航宿主：根据 [TaskFlowNavigator] 当前栈顶渲染页面。
 */
@Composable
fun TaskFlowNavHost(
    registry: TaskFlowRouteRegistry,
    startRoute: TaskFlowRoute,
    startArguments: TaskFlowRouteArguments = TaskFlowRouteArguments(),
) {
    var currentRoute by remember { mutableStateOf(startRoute) }
    var currentArgs by remember { mutableStateOf(startArguments) }

    val navigator = remember(registry) {
        TaskFlowNavigator(
            registry = registry,
            onStackChanged = { route, args ->
                currentRoute = route
                currentArgs = args
            },
        )
    }

    LaunchedEffect(startRoute, startArguments) {
        navigator.start(startRoute, startArguments)
    }

    val entry = registry.find(currentRoute)
    entry?.content?.invoke(currentArgs)
}

/**
 * 在 Composable 树中提供 [TaskFlowNavigator] 访问（由宿主页面手动传入）。
 */
@Composable
fun TaskFlowNavHost(
    navigator: TaskFlowNavigator,
    registry: TaskFlowRouteRegistry,
    currentRoute: TaskFlowRoute,
    currentArguments: TaskFlowRouteArguments,
) {
    val entry = registry.find(currentRoute)
    entry?.content?.invoke(currentArguments)
}
