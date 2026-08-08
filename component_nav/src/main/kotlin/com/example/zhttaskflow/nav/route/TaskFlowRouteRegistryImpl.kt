package com.example.zhttaskflow.nav.route

/**
 * 默认路由注册表实现。
 */
class TaskFlowRouteRegistryImpl : TaskFlowRouteRegistry {

    private val routeEntries = mutableListOf<TaskFlowRouteEntry>()

    override fun register(entry: TaskFlowRouteEntry) {
        routeEntries.add(entry)
    }

    override fun entries(): List<TaskFlowRouteEntry> = routeEntries.toList()
}
