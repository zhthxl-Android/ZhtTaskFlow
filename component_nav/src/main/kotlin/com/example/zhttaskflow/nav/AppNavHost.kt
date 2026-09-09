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
import androidx.navigation.compose.NavHost as NavigationNavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.zhttaskflow.nav.route.RouteRegistry
import com.example.zhttaskflow.nav.R
import com.example.zhttaskflow.nav.router.RouterInterceptorChain
import com.example.zhttaskflow.nav.router.rememberRouterInterceptUiBridge
import com.example.zhttaskflow.nav.transition.NavTransitionRegistry
import com.example.zhttaskflow.nav.transition.navEnterTransition
import com.example.zhttaskflow.nav.transition.navExitTransition
import com.example.zhttaskflow.nav.transition.navPopEnterTransition
import com.example.zhttaskflow.nav.transition.navPopExitTransition

/**
 * 统一 Navigation Compose 宿主：根据 [RouteRegistry] 装配导航图，并应用全局转场规范。
 *
 * - 绑定 [AppNavigator] 与拦截链协程作用域；非空 [routerInterceptorChain] 时跳转走深链（200）→ 权限（150）→ 登录（100）链。
 * - 拦截失败提示经 [com.example.zhttaskflow.nav.router.rememberRouterInterceptUiBridge] 注入 Snackbar。
 * - 业务 Composable 通过 [LocalNavigator] 获取 Navigator，禁止直接使用 [NavHostController]。
 *
 * 架构说明：[com.example.zhttaskflow.nav.doc.NavArchitecture]。
 *
 * 默认区分一级 Tab 切换与二级页面推入/弹出；可通过 [transitionRegistry] 按路由覆盖。
 * 若需完全自定义，可传入非 null 的 [enterTransition] 等 lambda（将替代对应默认实现）。
 */
@Composable
fun AppNavHost(
    registry: RouteRegistry,
    startDestination: String,
    navigator: AppNavigator,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    transitionRegistry: NavTransitionRegistry = NavTransitionRegistry.Default,
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? = null,
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? = null,
    popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? = null,
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? = null,
    routerInterceptorChain: RouterInterceptorChain = RouterInterceptorChain.Empty,
) {
    val interceptScope = rememberCoroutineScope()
    val defaultRouteError = stringResource(id = R.string.nav_str_route_intercept_failed)
    val interceptUiBridge = rememberRouterInterceptUiBridge(defaultErrorMessage = defaultRouteError)
    val resolvedInterceptorChain = remember(routerInterceptorChain, interceptUiBridge, defaultRouteError) {
        if (routerInterceptorChain.isEmpty) {
            RouterInterceptorChain.Empty
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
    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavigationNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier,
            enterTransition = enterTransition ?: {
                navEnterTransition(registry = transitionRegistry)
            },
            exitTransition = exitTransition ?: {
                navExitTransition(registry = transitionRegistry)
            },
            popEnterTransition = popEnterTransition ?: {
                navPopEnterTransition(registry = transitionRegistry)
            },
            popExitTransition = popExitTransition ?: {
                navPopExitTransition(registry = transitionRegistry)
            },
        ) {
            registry.entries().forEach { entry ->
                entry.register(this, navController)
            }
        }
    }
}

@Composable
fun rememberNavigator(): AppNavigator = remember { AppNavigator() }
