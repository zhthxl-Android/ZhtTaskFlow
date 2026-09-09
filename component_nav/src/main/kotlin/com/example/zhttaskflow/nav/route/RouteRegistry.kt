package com.example.zhttaskflow.nav.route

/**
 * 路由注册表：Feature 模块通过本接口注册页面，由宿主 [com.example.zhttaskflow.nav.AppNavHost] 统一装配。
 */
interface RouteRegistry {

    /** 注册一条路由 */
    fun register(entry: RouteEntry)

    /** 已注册条目（只读） */
    fun entries(): List<RouteEntry>
}
