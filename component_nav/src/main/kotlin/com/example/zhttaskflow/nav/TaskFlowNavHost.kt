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

/**
 * 统一 Navigation Compose 宿主：根据 [TaskFlowRouteRegistry] 装配导航图。
 *
 * @param enterTransition 进入动画（默认无），壳工程可注入 Tab 切换过渡
 * @param exitTransition 退出动画
 * @param popEnterTransition 返回栈 pop 时进入动画
 * @param popExitTransition 返回栈 pop 时退出动画
 */
@Composable
fun TaskFlowNavHost(
    registry: TaskFlowRouteRegistry,
    startDestination: String,
    navigator: TaskFlowNavigator,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { EnterTransition.None },
    exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { ExitTransition.None },
    popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        enterTransition,
    popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        exitTransition,
) {
    navigator.bind(navController)
    CompositionLocalProvider(LocalTaskFlowNavigator provides navigator) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            popEnterTransition = popEnterTransition,
            popExitTransition = popExitTransition,
        ) {
            registry.entries().forEach { entry ->
                entry.register(this, navController)
            }
        }
    }
}

@Composable
fun rememberTaskFlowNavigator(): TaskFlowNavigator = remember { TaskFlowNavigator() }
