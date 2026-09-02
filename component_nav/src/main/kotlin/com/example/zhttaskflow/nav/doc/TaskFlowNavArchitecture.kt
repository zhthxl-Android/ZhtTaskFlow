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
 * 1. **深链** `200` — [com.example.zhttaskflow.nav.interceptor.TaskFlowDeepLinkInterceptor]：
 *    [com.example.zhttaskflow.nav.interceptor.TaskFlowRouteDeepLinkMarker.wrap] → 映射为内部 path →
 *    [com.example.zhttaskflow.nav.router.TaskFlowRouteInterceptResult.Redirect] 后 **继续** 同一拦截链（见下「深链门禁」）。
 * 2. **权限** `150`（`LOGIN + 50`）— [com.example.zhttaskflow.nav.interceptor.TaskFlowPermissionInterceptor]：
 *    [com.example.zhttaskflow.nav.interceptor.TaskFlowRoutePermissionMarker.withPermissionGroup]。
 * 3. **登录** `100` — [com.example.zhttaskflow.nav.interceptor.TaskFlowLoginInterceptor]：
 *    [com.example.zhttaskflow.nav.interceptor.TaskFlowRouteAuthMarker.withNeedLogin]。
 *
 * 失败提示经 [com.example.zhttaskflow.nav.router.rememberTaskFlowRouterInterceptUiBridge] → 全局 Snackbar（Error）；
 * 与 ViewModel [com.example.zhttaskflow.base.mvi.BaseUiEffect] **无关**。
 *
 * ## 与 MVI 导航 Effect 的配合
 *
 * - ViewModel 下发 **纯净** path（不含 `needLogin` / `permissionGroup` query）。
 * - RouteHost 在 `navigator.navigate(...)` 前调用 [com.example.zhttaskflow.nav.interceptor.TaskFlowRouteGatePolicy.enrichNavigationPath]（见 `docs/TASKFLOW_ROUTE_GATES.md`）。
 * - 深链入口在 Activity / 壳层将 URI 转为 [com.example.zhttaskflow.nav.interceptor.TaskFlowRouteDeepLinkMarker] 再 `navigate`。
 *
 * ## 深链门禁策略
 *
 * 外部深链与 App 内跳转 **共用** [com.example.zhttaskflow.nav.TaskFlowNavigator.navigate] + 默认拦截链，行为一致：
 *
 * - 深链拦截器仅负责 URI → 内部 path；`Redirect` 之后仍按 priority 执行权限、登录等后续拦截器。
 * - `target` 应编码与 RouteHost 一致的 path（可含 `needLogin` / `permissionGroup` query，或映射后再由 RouteHost 包裹的等价标记），
 *   从而自动继承目标页的登录 / 权限门禁；未带标记的纯净 path 与未标记的内链相同，不做额外校验。
 *
 * ## 壳层横切能力注入（无 DI）
 *
 * | 能力 | 扩展方式 |
 * |------|----------|
 * | 拦截链 | [com.example.zhttaskflow.nav.interceptor.rememberTaskFlowAppRouterInterceptorChain] 的 `loginSession` / `deepLinkRouteMapper` / `permissionGrantChecker`；应用壳在装配 [com.example.zhttaskflow.nav.TaskFlowNavHost] 时传入自定义链 |
 * | 埋点 | [com.example.zhttaskflow.base.analytics.LocalTaskFlowAnalytics] / [com.example.zhttaskflow.base.analytics.TaskFlowAnalyticsCompositionRoot]（由 [com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold] 包裹） |
 * | Navigator | [com.example.zhttaskflow.nav.LocalTaskFlowNavigator]（[com.example.zhttaskflow.nav.TaskFlowNavHost] 提供） |
 * | Snackbar / Loading / Dialog | [com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold] 内 CompositionLocal；内层 Scaffold 经 [com.example.zhttaskflow.base.ui.taskFlowParentGlobalHostsOrNull] 继承 |
 *
 * 展示类 Effect（Snackbar 等）仍在 Screen 消费，见 [com.example.zhttaskflow.base.ext.TaskFlowUiEffectConsumption]。
 *
 * @see com.example.zhttaskflow.base.doc.TaskFlowBaseArchitecture
 */
object TaskFlowNavArchitecture
