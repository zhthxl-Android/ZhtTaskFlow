package com.example.zhttaskflow.nav.route

import androidx.compose.runtime.Composable

/**
 * 单条路由注册项：路径 + Composable 内容工厂。
 */
data class TaskFlowRouteEntry(
    val route: TaskFlowRoute,
    val content: @Composable (TaskFlowRouteArguments) -> Unit,
)

/**
 * 路由参数容器，支持字符串键值传递。
 */
data class TaskFlowRouteArguments(
    val params: Map<String, String> = emptyMap(),
) {
    fun getString(key: String): String? = params[key]
}
