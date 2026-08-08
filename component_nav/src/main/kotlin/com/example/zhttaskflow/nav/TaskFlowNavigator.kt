package com.example.zhttaskflow.nav

import com.example.zhttaskflow.nav.route.TaskFlowRoute
import com.example.zhttaskflow.nav.route.TaskFlowRouteArguments
import com.example.zhttaskflow.nav.route.TaskFlowRouteEntry
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry

/**
 * 导航控制器：管理回退栈、跳转与结果回调，由壳工程或 Feature 手动持有。
 */
class TaskFlowNavigator(
    private val registry: TaskFlowRouteRegistry,
    private val onStackChanged: (TaskFlowRoute, TaskFlowRouteArguments) -> Unit,
) {
    private val backStack = ArrayDeque<Pair<TaskFlowRoute, TaskFlowRouteArguments>>()
    private var resultCallback: ((String?) -> Unit)? = null

    val currentRoute: TaskFlowRoute?
        get() = backStack.lastOrNull()?.first

    fun start(route: TaskFlowRoute, arguments: TaskFlowRouteArguments = TaskFlowRouteArguments()) {
        backStack.clear()
        pushInternal(route, arguments)
    }

    fun navigate(route: TaskFlowRoute, arguments: TaskFlowRouteArguments = TaskFlowRouteArguments()) {
        pushInternal(route, arguments)
    }

    fun pop(): Boolean {
        if (backStack.size <= 1) return false
        backStack.removeLast()
        val (route, args) = backStack.last()
        onStackChanged(route, args)
        return true
    }

    fun popWithResult(result: String?) {
        resultCallback?.invoke(result)
        resultCallback = null
        pop()
    }

    fun navigateForResult(
        route: TaskFlowRoute,
        arguments: TaskFlowRouteArguments = TaskFlowRouteArguments(),
        onResult: (String?) -> Unit,
    ) {
        resultCallback = onResult
        navigate(route, arguments)
    }

    fun resolveCurrentEntry(): TaskFlowRouteEntry? {
        val route = currentRoute ?: return null
        return registry.find(route)
    }

    private fun pushInternal(route: TaskFlowRoute, arguments: TaskFlowRouteArguments) {
        requireNotNull(registry.find(route)) { "未注册路由: ${route.path}" }
        backStack.addLast(route to arguments)
        onStackChanged(route, arguments)
    }
}
