package com.example.zhttaskflow.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import com.example.zhttaskflow.nav.route.TaskFlowArticleNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowHomeNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowTaskNavRoutes

private const val MAIN_TAB_ANIM_DURATION_MS = 300
private val MainTabAnimEasing = FastOutSlowInEasing

private val MAIN_TAB_ROUTES: Set<String> = setOf(
    TaskFlowHomeNavRoutes.HOME_ROUTE,
    TaskFlowArticleNavRoutes.ARTICLE_LIST,
    TaskFlowTaskNavRoutes.TASK_LIST,
)

private fun isMainTabRoute(route: String?): Boolean {
    return route != null && MAIN_TAB_ROUTES.contains(route)
}

/**
 * Tab 间切换：淡入 + 轻微上移入场（与 [AnimatedContent] 主流策略一致，由 NavHost 过渡承载）。
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.mainTabEnterTransition(): EnterTransition {
    val fromTab = isMainTabRoute(initialState.destination.route)
    val toTab = isMainTabRoute(targetState.destination.route)
    if (fromTab && toTab) {
        return fadeIn(
            animationSpec = tween(durationMillis = MAIN_TAB_ANIM_DURATION_MS, easing = MainTabAnimEasing),
        ) + slideInVertically(
            animationSpec = tween(durationMillis = MAIN_TAB_ANIM_DURATION_MS, easing = MainTabAnimEasing),
            initialOffsetY = { fullHeight -> fullHeight / 12 },
        )
    }
    return fadeIn(
        animationSpec = tween(durationMillis = MAIN_TAB_ANIM_DURATION_MS, easing = MainTabAnimEasing),
    )
}

/**
 * Tab 间切换：淡出 + 轻微下移退场。
 */
fun AnimatedContentTransitionScope<NavBackStackEntry>.mainTabExitTransition(): ExitTransition {
    val fromTab = isMainTabRoute(initialState.destination.route)
    val toTab = isMainTabRoute(targetState.destination.route)
    if (fromTab && toTab) {
        return fadeOut(
            animationSpec = tween(durationMillis = MAIN_TAB_ANIM_DURATION_MS, easing = MainTabAnimEasing),
        ) + slideOutVertically(
            animationSpec = tween(durationMillis = MAIN_TAB_ANIM_DURATION_MS, easing = MainTabAnimEasing),
            targetOffsetY = { fullHeight -> fullHeight / 12 },
        )
    }
    return fadeOut(
        animationSpec = tween(durationMillis = MAIN_TAB_ANIM_DURATION_MS, easing = MainTabAnimEasing),
    )
}
