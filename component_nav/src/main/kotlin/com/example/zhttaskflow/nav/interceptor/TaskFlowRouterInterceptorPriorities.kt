package com.example.zhttaskflow.nav.interceptor

/**
 * 应用默认路由拦截链的 [com.example.zhttaskflow.nav.router.TaskFlowRouterInterceptor.priority] 常量。
 *
 * **排序规则**：数值 **越大越先** 执行（见 [com.example.zhttaskflow.nav.router.TaskFlowRouterInterceptorChain]）。
 *
 * ## 默认链执行顺序（与 [rememberTaskFlowAppRouterInterceptorChain] 装配一致）
 *
 * | 顺序 | 拦截器 | priority | 说明 |
 * |------|--------|----------|------|
 * | 1 | [TaskFlowDeepLinkInterceptor] | [DEEP_LINK]（200） | 解析 `@deeplink/`、`Redirect` 为内部 path 后继续链 |
 * | 2 | [TaskFlowPermissionInterceptor] | `LOGIN + 50`（150） | 剥离 `permissionGroup`；**高于**登录，先处理权限 |
 * | 3 | [TaskFlowLoginInterceptor] | [LOGIN]（100） | 剥离 `needLogin` |
 *
 * [PERMISSION]（80）为预留常量，**当前** [TaskFlowPermissionInterceptor] 使用 `LOGIN + 50`，勿与运行顺序混淆。
 *
 * @see com.example.zhttaskflow.nav.doc.TaskFlowNavArchitecture
 */
object TaskFlowRouterInterceptorPriorities {
    const val DEEP_LINK: Int = 200
    const val LOGIN: Int = 100
    /** 预留；权限拦截器实际 priority 为 [LOGIN] + 50。 */
    const val PERMISSION: Int = 80
}
