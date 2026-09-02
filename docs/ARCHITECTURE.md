# ZhtTaskFlow 架构说明

> 工程级约定与模块边界；UI 平台细节见 [TaskFlowBaseArchitecture]、路由与拦截见 [TaskFlowNavArchitecture]（`component_base` / `component_nav` 源码 KDoc）。

## 1. 目标

多模块 **Feature 垂直 DDD + Clean Architecture**（各 Feature 内嵌 `domain` / `data` / `presentation` / `navigation`）+ **标准 MVI**（`BaseUiState` / `BaseUiEvent` / `BaseUiEffect` / `BaseViewModel`）。壳工程 `app` 仅组装路由、全局宿主与深链入口。

**当前阶段结论（L3+）**：组件化边界清晰、路由拦截链与全局 UI 宿主可演示可扩展；埋点已抽象为 `TaskFlowAnalytics`，调试与产品实现可切换；权限/登录/深链在任务详情等页面有完整样板。尚未引入 DI 框架，依赖 **CompositionLocal + 手动 Factory** 托管横切能力。

## 2. 模块职责

| 模块 | namespace（自动） | 职责 |
|------|-------------------|------|
| app | com.example.zhttaskflow | 壳工程、NavHost、拦截链、深链 Intent、`AppMainShell` |
| component_base | com.example.zhttaskflow.base | MVI 基类、Compose 脚手架、Snackbar/Loading/Dialog/BottomSheet、Inset/IME、埋点抽象 |
| component_core | com.example.zhttaskflow.core | 网络、Room、日志、NetworkChecker |
| component_nav | com.example.zhttaskflow.nav | Navigation 封装、路由常量、拦截器链 |
| feature_home / feature_task / feature_article | com.example.zhttaskflow.feature.* | 业务自治（Clean 三层 + MVI + 路由注册） |

禁止顶层 `lib-domain`、`lib-data`；禁止 `feature_*` 互相依赖。

## 3. SDK 与工具链

- AGP 8.7.x，Gradle 8.9，Kotlin 2.0 + KSP
- compileSdk / targetSdk 由 `ConfigureAndroidCommon` 统一（当前 35）
- minSdk 24，JDK 17

## 4. namespace 自动生成

- 根包固定 `com.example.zhttaskflow`
- 模块 path 去 `component_` 前缀、`_` → `.`，拼接到根包
- 由 `taskFlow.android.common` 写入 `android.namespace`

## 5. 依赖流向（允许）

- app → component_nav；条件 → feature_*
- component_nav **api** → component_base
- component_core **api** → component_base
- feature_* → component_core、component_nav（base 经 api 传递）

## 6. 禁止依赖

- feature ↔ feature
- base / core / nav → feature 或 app
- core → nav
- core → 任意 Compose 坐标
- domain 层 → Android / Compose / nav

## 7. presentation 与 MVI

| 概念 | 约定 |
|------|------|
| UiState | `BaseUiState<T>`，Screen 用 `StateBox` 四态 |
| UiEvent | 密封类，仅 `ViewModel.onEvent` 入口 |
| UiEffect | 密封类；**导航**与**展示**分标记接口，双 Collector |
| 用户提示 | `ShowSnackbar` + 全局 Snackbar，**不用 Toast** |
| 跨页跳转 | ViewModel 发导航 Effect 或 path；RouteHost 调 `TaskFlowNavigator` |
| 跳转前门禁 | 登录/权限/深链：`TaskFlowRouterInterceptorChain`（非 UiEffect） |

双 Collector 与拦截链区别见 `TaskFlowUiEffectConsumption`、`TaskFlowRouterInterceptor` 文件头 KDoc。

## 8. 不引入 Hilt 与 CompositionLocal 托管

本工程 **不使用 Hilt / Koin 等 DI 框架**（面试与壳工程保持简单、显式依赖）。

| 能力 | 获取方式 | 提供位置 |
|------|----------|----------|
| Navigator | `LocalTaskFlowNavigator` / RouteHost 内 `rememberTaskFlowNavigator()` | `TaskFlowNavHost` |
| Snackbar | `rememberTaskFlowSnackbarDispatcher()` | `TaskFlowBaseScaffold` |
| Loading | `rememberTaskFlowLoadingController()` | 同上 |
| Dialog / BottomSheet | `rememberTaskFlowDialogController()` + `showConfirmDialog` / `showBottomSheet` | 同上 |
| 埋点 | `rememberTaskFlowAnalytics()` / `LocalTaskFlowAnalytics` | `TaskFlowAnalyticsCompositionRoot`（`TaskFlowBaseScaffold` 外层宿主） |
| ViewModel | 各 Feature `ViewModelProvider.Factory` 手动组装 UseCase | RouteHost |
| 登录会话 / 深链映射 / 权限校验 | `rememberTaskFlowAppRouterInterceptorChain(...)` 构造参数 | 应用壳装配 `TaskFlowNavHost` 时传入自定义链（默认 `AppMainShell` 使用默认 `remember`） |

**约定**：业务 Screen **禁止**直接持有 `NavHostController`；横切 UI **禁止**自建第二套 SnackbarHost/Dialog。内层 `TaskFlowListScaffold` 通过 `taskFlowParentGlobalHostsOrNull()` 继承父级宿主。

**CompositionLocal 扩展**：新增横切能力时优先增加 `compositionLocalOf` + 在 `TaskFlowBaseScaffold`（或壳层单一根节点）`CompositionLocalProvider` 注入；业务通过 `rememberXxx()` / `LocalXxx.current` 消费，避免在 Screen 传递长参数列表。主题等可选能力见 `LocalTaskFlowThemeController`。

替换产品埋点：实现 `TaskFlowAnalytics`，在壳层 `TaskFlowAnalyticsCompositionRoot(analytics = …)` 注入，业务仍调用 `logUiInteraction` / `PageLifecycleLog`，零改动。

## 9. 全局组件使用约定（component_base）

| 场景 | 组件 / API | 说明 |
|------|------------|------|
| 全局宿主 | `TaskFlowBaseScaffold` | 单 Snackbar 队列、Loading 遮罩、`TaskFlowDialogHost` |
| 一级列表 / Tab | `TaskFlowListScaffold` | `title` / `actions` / FAB；可折叠顶栏 |
| 二级详情 | `TaskFlowScaffold` | 顶栏返回 + `onBackIntercept`（WebView 内后退等） |
| 确认弹窗 | `TaskFlowConfirmDialog` 或 `showConfirmDialog` | 登录/权限引导等走 DialogController |
| 底部弹窗 | `showBottomSheet` + `TaskFlowBottomSheet` | 任务列表「更多」为样板；圆角/拖拽/动画由基建统一 |
| 列表状态 | `StateBox` + `TaskFlowStateRefreshableListContent` 等 | contentPadding 用 `rememberTaskFlowStateBoxContentPadding` |
| 表单键盘 | `rememberTaskFlowImePadding` | 弹窗内 `taskFlowImePadding` |

顶栏统一使用 [TaskFlowTopBar](component_base/src/main/kotlin/com/example/zhttaskflow/base/ui/TaskFlowTopBar.kt) / `TaskFlowListScaffold`；历史 `TaskFlowPageTitleBar` 已自源码移除，尺寸见 `TaskFlowUiConstants.TopBarHeight`。

## 10. 二级页返回规范

1. **统一外壳**：二级页使用 `TaskFlowScaffold`，`onNavigateUp` 绑定 `navigator.navigateUp()`。
2. **系统返回键**：由 Scaffold 内 `BackHandler` 与顶栏返回共用 `handleTaskFlowPageBack(onNavigateUp, onBackIntercept)`。
3. **可消费后退的容器**（如 WebView）：在 `onBackIntercept` 中若 `webView.canGoBack()` 则 `goBack()` 并返回 `true`；否则返回 `false` 走 `navigateUp`。参考 `ArticleDetailScreen`。
4. **一级 Tab 根页**：`TaskFlowListScaffold(interceptTabRootBackToDesktop = true)`，返回退桌面而非销毁进程。

禁止在 Screen 内单独再挂一层 `BackHandler` 与 Scaffold 冲突。

## 11. 交互日志与埋点规范

### 11.1 三类必埋（团队标准）

规范定义于 `ComposeInteractionLogging.kt` KDoc，底层统一走 **`TaskFlowAnalytics`**（默认 `TaskFlowDebugAnalytics` → `TaskFlowLogger`）。

| 类型 | API | 要求 |
|------|-----|------|
| 页面曝光 | `PageLifecycleLog(pageName, pageArgs)` | `pageName` 与 Screen 一致（如 `Home`、`TaskList`） |
| 核心 CTA | `logUiInteraction` / `clickWithLog` / `listItemClickWithLog` | 必传 `pageId`（与 `pageName` 对齐）；`opId` 命名 `{page}_{控件}` |
| 操作结果 | `logUiInteraction(action = success\|failure\|info, …)` | 在 Snackbar 等反馈处；`params` 可带 `message`，勿记敏感信息 |

**日志格式**（Debug 与改造前一致）：

```text
action=click pageId=TaskList opId=task_list_more params=...
```

页面生命周期：

```text
onEnter page=TaskList args=count=3
onLeave page=TaskList
```

### 11.2 业务扩展埋点（可选）

```kotlin
val analytics = rememberTaskFlowAnalytics()
analytics.trackUiClick(operationId = "home_entrance_card", pageId = "Home", params = mapOf("entranceId" to id))
```

## 12. 路由与拦截链（component_nav）

- 注册：`registerXxxRoutes(TaskFlowRouteRegistry, navigator)`
- 常量：`TaskFlowHomeNavRoutes`、`TaskFlowTaskNavRoutes`、`TaskFlowArticleNavRoutes`
- 默认链：`rememberTaskFlowAppRouterInterceptorChain()`（装配于 `AppMainShell`）
- 调试壳：`TaskFlowFeatureDebugShell` 与 App 壳行为对齐

**执行顺序**（`priority` 越大越先）：

1. 深链 `200` — `TaskFlowDeepLinkInterceptor`
2. 权限 `150`（`LOGIN + 50`）— 先剥离 `permissionGroup`，再与登录 query 共存
3. 登录 `100` — `TaskFlowLoginInterceptor`

失败：`Abort` → 拦截链 UI 桥 → 全局 Snackbar（Error）；与 ViewModel Effect 无关。

### 12.1 深链接入

**运营 URL 格式**（`TaskFlowDeepLinkRouteMapperImpl` 默认）：

```text
taskflow://nav/route?target=<URL 编码的内部 path>
```

示例：

```text
taskflow://nav/route?target=feature_task%2Fdetail%2Fdemo-1
```

**壳工程（`MainActivity`）**：`ACTION_VIEW` → 队列 → `AppMainShell` 组合完成后：

```kotlin
navigator.navigate(TaskFlowRouteDeepLinkMarker.wrap(uriString))
```

Manifest 声明 `taskflow` scheme；`launchMode=singleTop` + `onNewIntent`。桌面启动无 `data`，不受影响。

**门禁与内链一致**：深链解析后仍走同一套 `TaskFlowNavigator.navigate` 拦截链；`Redirect` 为内部 path 后继续执行权限（150）与登录（100）。`target` 应 URL 编码与 RouteHost 等价的 path——可含 `needLogin` / `permissionGroup` query（或映射规则产出已包裹的 path），从而自动继承目标页登录 / 权限标记；纯净 path 与未标记的内链一样不做额外门禁。

**adb 验证**：

```bat
adb shell am start -a android.intent.action.VIEW -d "taskflow://nav/route?target=feature_task%%2Fdetail%%2Fdemo-1" com.example.zhttaskflow
```

### 12.2 登录拦截（RouteHost 标记）

ViewModel 只下发纯净 path；RouteHost 跳转前包裹：

```kotlin
navigator.navigate(
    TaskFlowRouteAuthMarker.withNeedLogin(TaskFlowTaskNavRoutes.detailPath(taskId))
)
```

未登录：全局 `TaskFlowDialogController` 引导 → 模拟登录 → Snackbar 成功 → 自动继续跳转。

### 12.3 权限拦截（RouteHost 标记 + 模拟授权）

样板：**任务详情**叠加登录 + 存储权限（`feature_task` / `TaskRoute.kt`）：

```kotlin
navigator.navigate(
    navigationPathRequireStoragePermission(
        navigationPathRequireLogin(effect.url),
    ),
)
```

- 标记：`TaskFlowRoutePermissionMarker.withStoragePermission(route)` 或 `withPermissionGroup(route, groupId)`
- 未授权：`TaskFlowDialogController` 确认弹窗 → **模拟授权**（`TaskFlowPermissionDemoSession`，可替换为系统 Permission API）
- 拒绝：Snackbar Error；`Abort` 不重复弹 Toast

未标记路由 **不经过** 权限拦截，行为与改造前一致。

## 13. 六大能力矩阵（当前 L3+）

| 维度 | 等级 | 现状摘要 |
|------|------|----------|
| 组件化与 Clean 分层 | L3+ | Feature 垂直三层 + 路由注册；无 feature 互依；domain 无 Android |
| MVI 与 Effect 规范 | L3+ | 双 Collector；导航/展示分离；Snackbar 统一无 Toast |
| 路由与拦截 | L3+ | 深链 / 登录 / 权限链可演示；任务详情为门禁样板；深链从 MainActivity 打通 |
| UI 平台与脚手架 | L3+ | 单宿主 Scaffold；列表/详情/IME/BottomSheet 有规范与样例 |
| 可观测性（日志/埋点） | L3+ | PageLifecycle + 交互三类必埋；`TaskFlowAnalytics` 可替换实现 |
| 工程化与构建 | L3 | Version Catalog、build-logic、双模式 Feature；无 Hilt；依赖规则 `checkDependencyRules` |

**L3+ 含义**：关键横切能力有统一抽象、可运行样板与文档，新人可按文档复制接入；产品级 SDK（真实埋点、系统权限）预留扩展点，尚未全部落地。

## 14. Feature 双模式

| 模式 | gradle.properties | 说明 |
|------|-------------------|------|
| library | `feature.*.standalone=false` | 集成到 app |
| application | `true` | 独立 Debug Activity + 同一套路由/拦截链 |

## 15. 资源前缀

app_、base_、core_、nav_、task_、article_、home_ 等；Lint `MissingPrefix` 为 error。

## 16. 构建验证

```bat
.\gradlew.bat clean :app:compileDebugKotlin
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintVitalRelease
.\gradlew.bat checkDependencyRules
```

## 17. 新人 Onboarding 速查

1. 新页面：MVI 四件套 + `PageLifecycleLog` + 核心按钮 `logUiInteraction`（带 `pageId`）。
2. 新路由：在 `component_nav` 增常量 → Feature `registerXxxRoutes` → RouteHost 消费导航 Effect。
3. 需登录/权限页：仅在 RouteHost `navigate` 前加 Marker，不改 ViewModel path。
4. 二级页：`TaskFlowScaffold` + 返回规范；WebView 用 `onBackIntercept`。
5. 弹窗：优先 `showBottomSheet` / `showConfirmDialog`，不自建 `ModalBottomSheet`（除非基建扩展）。
6. 读源码 KDoc：`TaskFlowBaseArchitecture`、`TaskFlowNavArchitecture`、`MainActivity`（深链）、`TaskRoute.kt`（登录+权限）、`TaskListScreen.kt`（BottomSheet）、`ComposeInteractionLogging.kt`（埋点规范）。

[TaskFlowBaseArchitecture]: ../component_base/src/main/kotlin/com/example/zhttaskflow/base/doc/TaskFlowBaseArchitecture.kt
[TaskFlowNavArchitecture]: ../component_nav/src/main/kotlin/com/example/zhttaskflow/nav/doc/TaskFlowNavArchitecture.kt
