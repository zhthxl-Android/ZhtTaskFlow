package com.example.zhttaskflow.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry
import com.example.zhttaskflow.nav.transition.TaskFlowNavTransitionRegistry
import com.example.zhttaskflow.nav.transition.taskFlowEnterTransition
import com.example.zhttaskflow.nav.transition.taskFlowExitTransition
import com.example.zhttaskflow.nav.transition.taskFlowPopEnterTransition
import com.example.zhttaskflow.nav.transition.taskFlowPopExitTransition

/**
 * 统一 Navigation Compose 宿主：根据 [TaskFlowRouteRegistry] 装配导航图，并应用全局转场规范。
 *
 * 默认区分一级 Tab 切换与二级页面推入/弹出；可通过 [transitionRegistry] 按路由覆盖。
 * 若需完全自定义，可传入非 null 的 [enterTransition] 等 lambda（将替代对应默认实现）。
 */
@Composable
fun TaskFlowNavHost(
    registry: TaskFlowRouteRegistry,
    startDestination: String,
    navigator: TaskFlowNavigator,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    transitionRegistry: TaskFlowNavTransitionRegistry = TaskFlowNavTransitionRegistry.Default,
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? = null,
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? = null,
    popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? = null,
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? = null,
) {
    navigator.bind(navController)
    CompositionLocalProvider(LocalTaskFlowNavigator provides navigator) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier,
            enterTransition = enterTransition ?: {
                taskFlowEnterTransition(registry = transitionRegistry)
            },
            exitTransition = exitTransition ?: {
                taskFlowExitTransition(registry = transitionRegistry)
            },
            popEnterTransition = popEnterTransition ?: {
                taskFlowPopEnterTransition(registry = transitionRegistry)
            },
            popExitTransition = popExitTransition ?: {
                taskFlowPopExitTransition(registry = transitionRegistry)
            },
        ) {
            registry.entries().forEach { entry ->
                entry.register(this, navController)
            }
        }
    }
}

@Composable
fun rememberTaskFlowNavigator(): TaskFlowNavigator = remember { TaskFlowNavigator() }
