package com.example.zhttaskflow.base.doc

/**
 * # component_base 平台层说明（S2/S3）
 *
 * 本模块提供 **无业务语义** 的 Compose UI 基座、MVI 抽象与全局交互宿主，供各 feature 模块复用。
 *
 * ## 分层与职责边界
 *
 * - **domain**（`feature_xxx/domain`）：UseCase、Entity、Repository 接口；禁止 Android / Compose。
 * - **data**（`feature_xxx/data`）：Repository 实现、DTO、数据源；禁止直接暴露给 UI。
 * - **presentation**（`feature_xxx/presentation`）：ViewModel、Screen、MVI 模型；禁止跨 Feature import。
 * - **navigation**（`feature_xxx/navigation`）：路由注册、RouteHost、导航类 Effect 消费；Navigation API 不得泄漏到 Screen。
 * - **component_base**（`base.ui` / `base.mvi` / `base.ext`）：脚手架、StateBox、Snackbar/Loading/Dialog；禁止业务路由与实体。
 *
 * Feature 仅依赖 `component_base`（经 nav/core api 传递）、`component_core`、`component_nav`；**禁止** Feature 互相依赖。
 *
 * ## UI 脚手架层级
 *
 * 1. [com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold]：全局 Snackbar / Loading / Dialog **单宿主**；[com.example.zhttaskflow.base.ui.TaskFlowInsetsPolicy] 统一 Edge-to-Edge。
 * 2. [com.example.zhttaskflow.base.ui.TaskFlowListScaffold]：一级 Tab / 列表页（下拉刷新、分页列表封装见同文件）。
 * 3. [com.example.zhttaskflow.base.ui.TaskFlowScaffold]：二级详情页顶栏 + 系统返回拦截（委托 [com.example.zhttaskflow.base.ui.TaskFlowPageScaffold]）。
 *
 * 内层 Scaffold 通过 [com.example.zhttaskflow.base.ui.taskFlowParentGlobalHostsOrNull] 继承外层宿主，避免重复 SnackbarHost。
 *
 * ## MVI 与用户反馈（S3）
 *
 * - 状态：[com.example.zhttaskflow.base.mvi.BaseUiState] + [com.example.zhttaskflow.base.ui.StateBox] 四态容器。
 * - 事件：[com.example.zhttaskflow.base.mvi.BaseUiEvent] → [com.example.zhttaskflow.base.mvi.BaseViewModel.onEvent]。
 * - 副作用：[com.example.zhttaskflow.base.mvi.BaseUiEffect]；**双 Collector** 规范见 [com.example.zhttaskflow.base.ext.TaskFlowUiEffectConsumption]。
 * - 轻量提示：统一 [com.example.zhttaskflow.base.ext.showSnackbar] / `ShowSnackbar` Effect，**不使用**系统 Toast API。
 *
 * ## 列表组件接入（摘要）
 *
 * - 列表页外壳：`TaskFlowListScaffold` + `StateBox` + `rememberTaskFlowStateBoxContentPadding()`。
 * - 仅下拉刷新：`TaskFlowStateRefreshableListContent` / `TaskFlowRefreshableListPayload`。
 * - 分页 + 加载更多：`TaskFlowStatePaginatedListContent` / `TaskFlowPaginatedListPayload` + `TaskFlowPaginationController`。
 * - 首屏骨架：默认 `TaskFlowListSkeletonLoading`；列表 contentPadding 使用 `rememberTaskFlowListLazyContentPadding`。
 *
 * ## Inset 与 IME
 *
 * - Scaffold 内容区不消费 IME；键盘避让由 [com.example.zhttaskflow.base.ui.rememberTaskFlowImePadding] 在表单/弹窗按需调用。
 * - 详情见 [com.example.zhttaskflow.base.ui.TaskFlowInsetsPolicy] 与 `TaskFlowContentInsets.kt`。
 *
 * ## CompositionLocal 与壳层替换（无 DI）
 *
 * 横切能力由 Compose [androidx.compose.runtime.CompositionLocal] 向下传递；**扩展**时实现对应接口或在壳层包一层
 * [androidx.compose.runtime.CompositionLocalProvider]，业务仍通过 `remember*` / `Local*` 访问，避免第二套宿主。
 *
 * | Local / API | 提供方 | 壳层替换方式 |
 * |-------------|--------|----------------|
 * | [com.example.zhttaskflow.base.analytics.LocalTaskFlowAnalytics] | [com.example.zhttaskflow.base.analytics.TaskFlowAnalyticsCompositionRoot]（[com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold] 内） | 实现 [com.example.zhttaskflow.base.analytics.TaskFlowAnalytics] 并传入 `analytics =` |
 * | Snackbar / Loading / Dialog | [com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold] | 一般无需替换；内层列表/详情 Scaffold 继承父级 [com.example.zhttaskflow.base.ui.taskFlowParentGlobalHostsOrNull] |
 * | [com.example.zhttaskflow.nav.LocalTaskFlowNavigator] | [com.example.zhttaskflow.nav.TaskFlowNavHost] | 由 NavHost 注入，业务禁止持有 NavController |
 * | 路由拦截链 | [com.example.zhttaskflow.nav.interceptor.rememberTaskFlowAppRouterInterceptorChain] | 应用壳传入 [com.example.zhttaskflow.nav.TaskFlowNavHost] 的 `routerInterceptorChain`（见 [com.example.zhttaskflow.nav.doc.TaskFlowNavArchitecture]） |
 *
 * @see com.example.zhttaskflow.nav.doc.TaskFlowNavArchitecture
 */
object TaskFlowBaseArchitecture
