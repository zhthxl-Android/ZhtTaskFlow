package com.example.zhttaskflow.nav.route

/**
 * 默认路由注册表实现，线程不安全，仅在主线程装配阶段使用。
 */
class TaskFlowRouteRegistryImpl : TaskFlowRouteRegistry {

    private val routes = mutableMapOf<String, TaskFlowRouteEntry>()

    override fun register(route: TaskFlowRoute, entry: TaskFlowRouteEntry) {
        routes[route.path] = entry
    }

    override fun find(route: TaskFlowRoute): TaskFlowRouteEntry? = routes[route.path]

    override fun allRoutes(): List<TaskFlowRoute> = routes.keys.map { TaskFlowRoute(it) }
}
