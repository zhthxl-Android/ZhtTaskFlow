package com.example.zhttaskflow.base.ext

import com.example.zhttaskflow.base.mvi.BaseUiEffect

/**
 * # MVI UiEffect 双 Collector 消费规范
 *
 * ViewModel 通过 [com.example.zhttaskflow.base.mvi.BaseViewModel.uiEffect] 广播一次性副作用；
 * **同一 Flow 允许两个订阅方**，按 Effect **分类**拆分职责，避免在单层 `when` 中混杂导航与页面 UI。
 *
 * ## 职责边界
 *
 * | 层级 | 订阅位置 | 处理类型 | 典型操作 |
 * |------|----------|----------|----------|
 * | **RouteHost** | `*Route.kt` 内 `LaunchedEffect` | [TaskFlowNavigationUiEffect] | `TaskFlowNavigator.navigate` / `navigateMainTab`、RouteHost 侧 path 标记（登录/权限） |
 * | **Screen** | `*Screen.kt` 内 Scaffold 子树 `LaunchedEffect` | [TaskFlowPresentationUiEffect] | Snackbar、全局 Loading、页面级 Dialog |
 *
 * ## 设计意图
 *
 * - **Route** 持有 [com.example.zhttaskflow.nav.LocalTaskFlowNavigator]，贴近导航图与依赖组装，适合消费跨页面跳转。
 * - **Screen** 位于 [com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold] 子树，可访问 Snackbar / Loading / Dialog 的 CompositionLocal。
 * - SharedFlow 多 collector：导航类在 Route 处理，展示类在 Screen 处理；**同一实例只会被一个分支匹配**，不构成重复执行。
 *
 * ## 新增 Effect  checklist
 *
 * 1. 在 Feature `*UiEffect` 密封类中声明，并 **implement** [TaskFlowNavigationUiEffect] 或 [TaskFlowPresentationUiEffect]（二选一）。
 * 2. RouteHost 的 `when` **仅**分支导航子类型；展示类走 `else` 或显式忽略并注释「由 Screen 消费」。
 * 3. Screen 的 `when` **仅**处理展示子类型；导航类注释「由 RouteHost 消费」。
 *
 * ## 与路由拦截链的区别
 *
 * [com.example.zhttaskflow.nav.router.TaskFlowRouterInterceptor] 处理的是 **跳转前** 的登录/权限/深链拦截，
 * 与 ViewModel 下发的 [BaseUiEffect] 无关；拦截结果通过 Navigator 与全局 Snackbar 展示，不经过 `uiEffect`。
 *
 * @see BaseUiEffect
 * @see TaskFlowNavigationUiEffect
 * @see com.example.zhttaskflow.nav.doc.TaskFlowNavArchitecture
 */
object TaskFlowUiEffectConsumption

/**
 * **导航类** UiEffect 标记：仅由 RouteHost（路由宿主 Composable）消费。
 *
 * 实现类应通过 [com.example.zhttaskflow.nav.TaskFlowNavigator] 完成跳转，不得在 Screen 层直接操作 NavController。
 */
interface TaskFlowNavigationUiEffect : BaseUiEffect

/**
 * **展示类** UiEffect 标记：仅由 Screen（页面 UI Composable，通常在 Scaffold 子树内）消费。
 *
 * 包括 Snackbar、阻塞 Loading、页面弹窗等页面内或全局托管的 UI 反馈，不包含跨页面路由跳转。
 */
interface TaskFlowPresentationUiEffect : BaseUiEffect
