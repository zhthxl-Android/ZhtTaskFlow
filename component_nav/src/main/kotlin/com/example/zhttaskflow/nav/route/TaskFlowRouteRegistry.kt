package com.example.zhttaskflow.nav.route

/**
 * 路由注册表：Feature 模块通过本接口注册页面，由宿主 [com.example.zhttaskflow.nav.TaskFlowNavHost] 统一装配。
 */
interface TaskFlowRouteRegistry {

    /** 注册一条路由 */
    fun register(entry: TaskFlowRouteEntry)

    /** 已注册条目（只读） */
    fun entries(): List<TaskFlowRouteEntry>
}
