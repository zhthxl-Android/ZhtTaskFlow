package com.example.zhttaskflow.nav

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry
import com.example.zhttaskflow.nav.R
import com.example.zhttaskflow.nav.router.TaskFlowRouterInterceptorChain
import com.example.zhttaskflow.nav.router.rememberTaskFlowRouterInterceptUiBridge
import com.example.zhttaskflow.nav.transition.TaskFlowNavTransitionRegistry
import com.example.zhttaskflow.nav.transition.taskFlowEnterTransition
import com.example.zhttaskflow.nav.transition.taskFlowExitTransition
import com.example.zhttaskflow.nav.transition.taskFlowPopEnterTransition
import com.example.zhttaskflow.nav.transition.taskFlowPopExitTransition

/**
 * 统一 Navigation Compose 宿主：根据 [TaskFlowRouteRegistry] 装配导航图，并应用全局转场规范。
 *
 * - 绑定 [TaskFlowNavigator] 与拦截链协程作用域；非空 [routerInterceptorChain] 时跳转走深链（200）→ 权限（150）→ 登录（100）链。
 * - 拦截失败提示经 [com.example.zhttaskflow.nav.router.rememberTaskFlowRouterInterceptUiBridge] 注入 Snackbar。
 * - 业务 Composable 通过 [LocalTaskFlowNavigator] 获取 Navigator，禁止直接使用 [NavHostController]。
 *
 * 架构说明：[com.example.zhttaskflow.nav.doc.TaskFlowNavArchitecture]。
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
    routerInterceptorChain: TaskFlowRouterInterceptorChain = TaskFlowRouterInterceptorChain.Empty,
) {
    val interceptScope = rememberCoroutineScope()
    val defaultRouteError = stringResource(id = R.string.nav_str_route_intercept_failed)
    val interceptUiBridge = rememberTaskFlowRouterInterceptUiBridge(defaultErrorMessage = defaultRouteError)
    val resolvedInterceptorChain = remember(routerInterceptorChain, interceptUiBridge, defaultRouteError) {
        if (routerInterceptorChain.isEmpty) {
            TaskFlowRouterInterceptorChain.Empty
        } else {
            routerInterceptorChain.withUiBridge(
                bridge = interceptUiBridge,
                defaultErrorMessage = defaultRouteError,
            )
        }
    }
    SideEffect {
        navigator.installRouterInterceptorChain(resolvedInterceptorChain)
    }
    navigator.bind(navController, interceptScope)
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
