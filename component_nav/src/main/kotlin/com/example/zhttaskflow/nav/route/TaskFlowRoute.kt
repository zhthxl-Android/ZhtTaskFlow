package com.example.zhttaskflow.nav.route

/**
 * 路由路径值对象，统一使用以 `/` 开头的路径规范，例如 `/main/home`。
 * 这个注解 @JvmInline 表示这是一个内联类，用于优化性能，减少对象创建开销。
 * value class 表示这是一个值类，它的实例在运行时会用底层类型（这里是 String）表示。
 */
@JvmInline
value class TaskFlowRoute(val path: String) {
    init {
        require(path.startsWith("/")) { "路由路径必须以 / 开头: $path" }
    }
}

/**
 * 路由表注册接口：Feature 模块在装配阶段向注册表添加页面，不写在 nav 组件内部。
 */
interface TaskFlowRouteRegistry {
/**
 * 注册任务流路由的函数
 * @param route 任务流路由对象，用于标识和定位特定的任务流
 * @param entry 任务流路由条目，包含任务流的具体信息和处理逻辑
 */
    fun register(route: TaskFlowRoute, entry: TaskFlowRouteEntry)
/**
 * 根据给定的任务流路由查找对应的任务流路由条目
 * @param route 任务流路由对象，用于查找匹配的路由条目
 * @return 返回匹配的TaskFlowRouteEntry对象，如果没有找到则返回null
 */
    fun find(route: TaskFlowRoute): TaskFlowRouteEntry?
/**
 * 获取所有可用的任务流程路由列表
 *
 * @return 返回一个包含所有TaskFlowRoute对象的List集合
 *         每个TaskFlowRoute代表一个可用的任务流程路由
 */
    fun allRoutes(): List<TaskFlowRoute>
}
