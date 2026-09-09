package com.example.zhttaskflow.nav.route

/**
 * 默认路由注册表实现。
 */
class RouteRegistryImpl : RouteRegistry {

    private val routeEntries = mutableListOf<RouteEntry>()

    override fun register(entry: RouteEntry) {
        routeEntries.add(entry)
    }

    override fun entries(): List<RouteEntry> = routeEntries.toList()
}
