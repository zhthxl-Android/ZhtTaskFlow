package com.example.zhttaskflow.nav.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import androidx.compose.ui.unit.IntOffset
import com.example.zhttaskflow.nav.route.ArticleNavRoutes
import com.example.zhttaskflow.nav.route.LogNavRoutes
import com.example.zhttaskflow.nav.route.TaskNavRoutes

/**
 * 全局 NavHost 转场时长与缓动（Material 推荐 [FastOutSlowInEasing]）。
 */
object NavTransitionDefaults {
    const val MAIN_TAB_DURATION_MS: Int = 300
    const val SECONDARY_DURATION_MS: Int = 350
    val Easing = FastOutSlowInEasing
}

private val MAIN_TAB_ROUTES: Set<String> = setOf(
    ArticleNavRoutes.ARTICLE_LIST,
    TaskNavRoutes.TASK_LIST,
    LogNavRoutes.LOG_ROUTE,
)

private fun isMainTabRoute(route: String?): Boolean {
    return route != null && MAIN_TAB_ROUTES.contains(route)
}

/**
 * 单路由转场覆盖（进入/退出/pop 可只配部分，未配则走全局策略）。
 */
data class RouteTransitionOverride(
    val enter: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? = null,
    val exit: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? = null,
    val popEnter: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition)? = null,
    val popExit: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition)? = null,
)

/**
 * 按路由（精确匹配或前缀匹配）注册特殊页面转场。
 */
class NavTransitionRegistry internal constructor(
    private val overrides: Map<String, RouteTransitionOverride>,
) {
    fun resolveEnter(route: String?): RouteTransitionOverride? = resolve(route)

    fun resolveExit(route: String?): RouteTransitionOverride? = resolve(route)

    private fun resolve(route: String?): RouteTransitionOverride? {
        if (route.isNullOrBlank()) {
            return null
        }
        overrides[route]?.let { return it }
        return overrides.entries.firstOrNull { entry ->
            route.startsWith(entry.key)
        }?.value
    }

    companion object {
        val Default: NavTransitionRegistry = NavTransitionRegistry(emptyMap())

        fun build(
            block: Builder.() -> Unit,
        ): NavTransitionRegistry = Builder().apply(block).build()
    }

    class Builder {
        private val map = linkedMapOf<String, RouteTransitionOverride>()

        fun route(
            routePattern: String,
            override: RouteTransitionOverride,
        ) {
            map[routePattern] = override
        }

        fun build(): NavTransitionRegistry = NavTransitionRegistry(map.toMap())
    }
}

private fun <T> tabTween(
    durationMs: Int = NavTransitionDefaults.MAIN_TAB_DURATION_MS,
): FiniteAnimationSpec<T> = tween(
    durationMillis = durationMs,
    easing = NavTransitionDefaults.Easing,
)

private fun <T> secondaryTween(): FiniteAnimationSpec<T> = tween(
    durationMillis = NavTransitionDefaults.SECONDARY_DURATION_MS,
    easing = NavTransitionDefaults.Easing,
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.mainTabEnter(): EnterTransition {
    return fadeIn(animationSpec = tabTween<Float>()) + slideInVertically(
        animationSpec = tabTween<IntOffset>(),
        initialOffsetY = { fullHeight -> fullHeight / 12 },
    )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.mainTabExit(): ExitTransition {
    return fadeOut(animationSpec = tabTween<Float>()) + slideOutVertically(
        animationSpec = tabTween<IntOffset>(),
        targetOffsetY = { fullHeight -> fullHeight / 12 },
    )
}

private fun secondaryForwardEnter(): EnterTransition {
    return slideInHorizontally(
        animationSpec = secondaryTween<IntOffset>(),
        initialOffsetX = { fullWidth -> fullWidth },
    ) + fadeIn(animationSpec = secondaryTween<Float>())
}

private fun secondaryForwardExit(): ExitTransition {
    return slideOutHorizontally(
        animationSpec = secondaryTween<IntOffset>(),
        targetOffsetX = { fullWidth -> -fullWidth / 4 },
    ) + fadeOut(animationSpec = secondaryTween<Float>())
}

private fun secondaryPopEnter(): EnterTransition {
    return slideInHorizontally(
        animationSpec = secondaryTween<IntOffset>(),
        initialOffsetX = { fullWidth -> -fullWidth / 4 },
    ) + fadeIn(animationSpec = secondaryTween<Float>())
}

private fun secondaryPopExit(): ExitTransition {
    return slideOutHorizontally(
        animationSpec = secondaryTween<IntOffset>(),
        targetOffsetX = { fullWidth -> fullWidth },
    ) + fadeOut(animationSpec = secondaryTween<Float>())
}

/**
 * NavHost 统一「前进进入」：Tab 互切 / 二级页推入 / 可被 [NavTransitionRegistry] 覆盖。
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.navEnterTransition(
    registry: NavTransitionRegistry = NavTransitionRegistry.Default,
): EnterTransition {
    val fromRoute = initialState.destination.route
    val toRoute = targetState.destination.route
    registry.resolveEnter(toRoute)?.enter?.invoke(this)?.let { return it }
    return when {
        isMainTabRoute(fromRoute) && isMainTabRoute(toRoute) -> mainTabEnter()
        !isMainTabRoute(toRoute) -> secondaryForwardEnter()
        else -> fadeIn(animationSpec = tabTween<Float>())
    }
}

/**
 * NavHost 统一「前进退出」。
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.navExitTransition(
    registry: NavTransitionRegistry = NavTransitionRegistry.Default,
): ExitTransition {
    val fromRoute = initialState.destination.route
    val toRoute = targetState.destination.route
    registry.resolveExit(fromRoute)?.exit?.invoke(this)?.let { return it }
    return when {
        isMainTabRoute(fromRoute) && isMainTabRoute(toRoute) -> mainTabExit()
        !isMainTabRoute(toRoute) -> secondaryForwardExit()
        else -> fadeOut(animationSpec = tabTween<Float>())
    }
}

/**
 * NavHost 统一「返回进入」（下层页面重新露出）。
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.navPopEnterTransition(
    registry: NavTransitionRegistry = NavTransitionRegistry.Default,
): EnterTransition {
    val fromRoute = initialState.destination.route
    val toRoute = targetState.destination.route
    registry.resolveEnter(toRoute)?.popEnter?.invoke(this)?.let { return it }
    return when {
        isMainTabRoute(toRoute) && !isMainTabRoute(fromRoute) -> secondaryPopEnter()
        isMainTabRoute(fromRoute) && isMainTabRoute(toRoute) -> mainTabEnter()
        else -> fadeIn(animationSpec = tabTween<Float>())
    }
}

/**
 * NavHost 统一「返回退出」（当前二级页滑出）。
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.navPopExitTransition(
    registry: NavTransitionRegistry = NavTransitionRegistry.Default,
): ExitTransition {
    val fromRoute = initialState.destination.route
    val toRoute = targetState.destination.route
    registry.resolveExit(fromRoute)?.popExit?.invoke(this)?.let { return it }
    return when {
        !isMainTabRoute(fromRoute) -> secondaryPopExit()
        isMainTabRoute(fromRoute) && isMainTabRoute(toRoute) -> mainTabExit()
        else -> fadeOut(animationSpec = tabTween<Float>())
    }
}
