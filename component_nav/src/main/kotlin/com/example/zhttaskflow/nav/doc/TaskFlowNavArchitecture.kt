package com.example.zhttaskflow.nav.doc

/**
 * # component_nav 路由与拦截层说明（S2/S3）
 *
 * Navigation Compose **仅允许在本模块内**直接使用；业务 Feature 通过 [com.example.zhttaskflow.nav.TaskFlowNavigator]、
 * [com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry] 与 `TaskFlow*NavRoutes` 常量跳转。
 *
 * ## 核心类型
 *
 * - [com.example.zhttaskflow.nav.TaskFlowNavHost]：装配 NavGraph、转场、注入拦截链 UI 桥。
 * - [com.example.zhttaskflow.nav.TaskFlowNavigator]：`navigate` / `navigateMainTab` / `navigateUp`。
 * - [com.example.zhttaskflow.nav.LocalTaskFlowNavigator]：Compose 内获取当前 Navigator。
 * - [com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry]：Feature `registerXxxRoutes` 注册入口。
 *
 * 路由 path 常量集中在 `component_nav` 的 `TaskFlow*NavRoutes`，Feature 禁止重复定义字符串。
 *
 * ## 路由拦截链（跳转前，非 UiEffect）
 *
 * 装配入口：[com.example.zhttaskflow.nav.interceptor.rememberTaskFlowAppRouterInterceptorChain]（App 壳与
 * [com.example.zhttaskflow.nav.standalone.TaskFlowFeatureDebugShell] 默认一致）。
 *
 * 执行顺序（[com.example.zhttaskflow.nav.interceptor.TaskFlowRouterInterceptorPriorities] 数值越大越先执行）：
 *
 * 1. **深链** — [com.example.zhttaskflow.nav.interceptor.TaskFlowDeepLinkInterceptor]：
 *    [com.example.zhttaskflow.nav.interceptor.TaskFlowRouteDeepLinkMarker.wrap] → 映射为内部 path。
 * 2. **登录** — [com.example.zhttaskflow.nav.interceptor.TaskFlowLoginInterceptor]：
 *    [com.example.zhttaskflow.nav.interceptor.TaskFlowRouteAuthMarker.withNeedLogin]。
 * 3. **权限** — [com.example.zhttaskflow.nav.interceptor.TaskFlowPermissionInterceptor]：
 *    [com.example.zhttaskflow.nav.interceptor.TaskFlowRoutePermissionMarker.withPermissionGroup]。
 *
 * 失败提示经 [com.example.zhttaskflow.nav.router.rememberTaskFlowRouterInterceptUiBridge] → 全局 Snackbar（Error）；
 * 与 ViewModel [com.example.zhttaskflow.base.mvi.BaseUiEffect] **无关**。
 *
 * ## 与 MVI 导航 Effect 的配合
 *
 * - ViewModel 下发 **纯净** path（不含 `needLogin` / `permissionGroup` query）。
 * - RouteHost 在 `navigator.navigate(...)` 前按需包裹 Auth / Permission 标记（见 `feature_task` 任务详情示范）。
 * - 深链入口在 Activity / 壳层将 URI 转为 [com.example.zhttaskflow.nav.interceptor.TaskFlowRouteDeepLinkMarker] 再 `navigate`。
 *
 * 展示类 Effect（Snackbar 等）仍在 Screen 消费，见 [com.example.zhttaskflow.base.ext.TaskFlowUiEffectConsumption]。
 *
 * @see com.example.zhttaskflow.base.doc.TaskFlowBaseArchitecture
 */
object TaskFlowNavArchitecture
