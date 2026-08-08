package com.example.zhttaskflow.nav.route

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

/**
 * 单条路由注册项：由 Nav 组件在 [com.example.zhttaskflow.nav.TaskFlowNavHost] 内展开为 Navigation 图节点。
 *
 * @param route 路由路径（可含占位参数）
 * @param register 在 NavGraph 上注册 composable 的逻辑（封装 Navigation API，业务层不直接调用）
 */
data class TaskFlowRouteEntry(
    val route: String,
    val register: NavGraphBuilder.(NavHostController) -> Unit,
)

/**
 * 无参数页面的便捷注册。
 */
fun simpleRouteEntry(
    route: String,
    content: @Composable () -> Unit,
): TaskFlowRouteEntry {
    return TaskFlowRouteEntry(
        route = route,
        register = {
            composable(route = route) {
                content()
            }
        },
    )
}

/**
 * 单 String 路径参数的页面注册。
 */
fun stringArgRouteEntry(
    route: String,
    argumentName: String,
    content: @Composable (argument: String) -> Unit,
): TaskFlowRouteEntry {
    return TaskFlowRouteEntry(
        route = route,
        register = {
            composable(
                route = route,
                arguments = listOf(
                    navArgument(argumentName) {
                        type = NavType.StringType
                    },
                ),
            ) { backStackEntry ->
                val value = backStackEntry.arguments?.getString(argumentName).orEmpty()
                content(value)
            }
        },
    )
}
