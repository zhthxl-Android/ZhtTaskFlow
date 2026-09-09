# ZhtTaskFlow 架构说明

> 工程级约定与模块边界；UI 平台细节见 [BaseArchitecture]、路由与拦截见 [NavArchitecture]（`component_base` / `component_nav` 源码 KDoc）。

## 1. 目标

多模块 **Feature 垂直 DDD + Clean Architecture**（各 Feature 内嵌 `domain` / `data` / `presentation` / `navigation`）+ **标准 MVI**（`BaseUiState` / `BaseUiEvent` / `BaseUiEffect` / `BaseViewModel`）。壳工程 `app` 仅组装路由、全局宿主与深链入口。

**当前阶段结论（L4 演进中）**：组件化边界清晰、路由拦截链与全局 UI 宿主可演示可扩展；埋点已抽象为 `Analytics`，页面性能已接入 `Performance`；权限/登录/深链在任务详情等页面有完整样板。尚未引入 DI 框架，依赖 **CompositionLocal + 手动 Factory** 托管横切能力。

## 2. 模块职责

| 模块 | namespace（自动） | 职责 |
|------|-------------------|------|
| app | com.example.zhttaskflow | 壳工程、NavHost、拦截链、深链 Intent、`AppMainShell` |
| component_base | com.example.zhttaskflow.base | MVI 基类、Compose 脚手架、Snackbar/Loading/Dialog/BottomSheet、Inset/IME、埋点抽象、页面性能 |
| component_core | com.example.zhttaskflow.core | 网络（`NetworkChecker.isNetworkConnected`）、Room（`RoomTemplate` / `RoomConfig`）、三级缓存 `ThreeTierCache`、DataStore（`PreferencesDataStoreFactory`）、**本地可观测日志仓**（`LocalLogStore`）、`RuntimeUtils` / `bindNetworkDiagnostics` |
| component_nav | com.example.zhttaskflow.nav | Navigation 封装、路由常量、拦截器链 |
| feature_log | com.example.zhttaskflow.feature.log | **日志查看**（`app/log` Tab，**本地可观测唯一 UI 入口**）：列表、筛选、导出、清空；MVI + `registerLogRoutes` |
| feature_task | com.example.zhttaskflow.feature.task | 任务列表/详情（Clean 三层 + MVI） |
| feature_article | com.example.zhttaskflow.feature.article | 资讯列表/WebView 详情（Clean 三层 + MVI） |

禁止顶层 `lib-domain`、`lib-data`；禁止 `feature_*` 互相依赖。

### 2.1 主壳底部 Tab（`AppMainShell`）

| 顺序 | Tab 文案 | 路由 path | 说明 |
|------|----------|-----------|------|
| 1 | 资讯 | `feature_article/list` | **默认启动页**（`MainTab.startDestinationRoute`）；`interceptTabRootBackToDesktop = true` |
| 2 | 任务 | `feature_task/list` | 任务列表根页 |
| 3 | 日志 | `app/log` | 日志查看（`feature_log`）；Debug/Release 均展示，无环境隐藏 |

配置类：`app` 模块 `MainTab`；底栏 UI：`MainBottomNavigationBar`。桌面 `MAIN`/`LAUNCHER` 启动无深链时进入 **资讯列表**（见 `MainActivity` KDoc）。

## 3. SDK 与工具链

- AGP 8.7.x，Gradle 8.9，Kotlin 2.0 + KSP
- compileSdk / targetSdk 由 `ConfigureAndroidCommon` 统一（当前 35）
- minSdk 24，JDK 17

## 4. namespace 自动生成

- 根包固定 `com.example.zhttaskflow`
- 模块 path 去 `component_` 前缀、`_` → `.`，拼接到根包
- 由 `taskFlow.android.common` 写入 `android.namespace`（build-logic 内部 `computeModuleNamespace()`）

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
| 跨页跳转 | ViewModel 发导航 Effect 或 path；RouteHost 调 `AppNavigator` |
| 跳转前门禁 | 登录/权限/深链：`RouterInterceptorChain`（非 UiEffect） |

双 Collector 与拦截链区别见 [UiEffectConsumption]、`RouterInterceptor` 文件头 KDoc。

## 8. 不引入 Hilt 与 CompositionLocal 托管

本工程 **不使用 Hilt / Koin 等 DI 框架**（面试与壳工程保持简单、显式依赖）。

| 能力 | 获取方式 | 提供位置 |
|------|----------|----------|
| Navigator | `LocalNavigator` / RouteHost 内 `rememberNavigator()` | `AppNavHost` |
| Snackbar | `rememberSnackbarDispatcher()` → 内部 `SnackbarDispatcher` | `BaseScaffold` |
| Loading | `rememberLoadingController()` → 内部 `LoadingController` | 同上 |
| Dialog / BottomSheet | `rememberDialogController()` → 内部 `DialogController` + `showConfirmDialog` / `showBottomSheet` | 同上 |
| 埋点 | `rememberAnalytics()` / `LocalAnalytics` | `AnalyticsCompositionRoot`（`BaseScaffold` 外层宿主） |
| 页面性能 | `rememberPerformance()` / `LocalPerformance` | `PerformanceCompositionRoot`（`BaseScaffold` 外层宿主） |
| 崩溃上报 | `LocalCrashReporter` / `CrashReporterRegistry` | `ExceptionMonitoringRoot`（`BaseScaffold` 拥有全局宿主时） |
| ViewModel | 各 Feature `ViewModelProvider.Factory` 手动组装 UseCase | RouteHost |
| 登录会话 / 深链映射 / 权限校验 | `rememberAppRouterInterceptorChain(...)` 构造参数 + `AppMainShell` 的 CompositionLocal | 应用壳装配 `AppNavHost` 时传入自定义链（默认 `AppMainShell` 使用默认 `remember`） |

**约定**：业务 Screen **禁止**直接持有 `NavHostController`；横切 UI **禁止**自建第二套 SnackbarHost/Dialog。内层 `ListScaffold` 通过 `parentGlobalHostsOrNull()` 继承父级宿主。

**CompositionLocal 扩展**：新增横切能力时优先增加 `compositionLocalOf` + 在 `BaseScaffold`（或壳层单一根节点）`CompositionLocalProvider` 注入；业务通过 `rememberXxx()` / `LocalXxx.current` 消费，避免在 Screen 传递长参数列表。主题等可选能力见 `LocalThemeController`。

### 8.1 壳层五类可配置注入（无 Hilt，开闭原则）

业务 Feature **不修改**即可切换产品实现；替换点在 **app 壳 `AppMainShell`**（推荐）或直接向 **`BaseScaffold`** 传入对应参数。

| 能力 | 抽象 / Local | Debug 默认 | Release 默认（`AppMainShell` 未传参时） | 壳层替换参数 | 传递链 |
|------|----------------|------------|----------------------------------------|--------------|--------|
| **埋点 Analytics** | `Analytics`、`LocalAnalytics` | `DebugAnalytics` | `ReleaseAnalytics` | `analyticsImpl` | `AppMainShell` → `BaseScaffold(analytics)` → `AnalyticsCompositionRoot` |
| **页面性能 APM** | `PerformanceReporter`、`LocalPerformance` | `DebugPerformanceReporter` | `ReleasePerformanceReporter` | `performanceImpl` | `AppMainShell` → `BaseScaffold(performanceImpl)` → `PerformanceCompositionRoot` |
| **崩溃上报** | `CrashReporter`、`LocalCrashReporter` | `DebugCrashReporter` | `ReleaseCrashReporter` | `crashReporterImpl` | `AppMainShell` → `BaseScaffold(crashReporter)` → `ExceptionMonitoringRoot` → `CrashReporterCompositionRoot` |
| **登录会话** | `LoginSession`、`LocalLoginSession` | 内存会话 | 同左 | `loginSessionImpl` | `CompositionLocalProvider` → `rememberAppRouterInterceptorChain(loginSession)` |
| **深链映射** | `DeepLinkRouteMapper`、`LocalDeepLinkRouteMapper` | `DeepLinkRouteMapperImpl` | 同左 | `deepLinkMapperImpl` | `CompositionLocalProvider` → 拦截链 `deepLinkRouteMapper` |

环境切换依据 **`isDebugLoggingEnabled()`**（与 `BuildConfig.DEBUG` 解耦）：Debug 安装包走调试实现三联；否则走 `app` 模块 `Release*` 实现（见 §8.2）。

**五类注入一键示例（app 壳）**：

```kotlin
AppMainShell(
    registry = routeRegistry,
    startDestination = MainTab.startDestinationRoute, // 默认：资讯列表 feature_article/list
    navigator = navigator,
    analyticsImpl = MyProductAnalytics(),           // 可选；默认 ReleaseAnalytics
    performanceImpl = MyApmPerformanceReporter(),   // 可选；默认 ReleasePerformanceReporter
    crashReporterImpl = MyBuglyCrashReporter(),     // 可选；默认 ReleaseCrashReporter
    loginSessionImpl = mySession,                   // 可选
    deepLinkMapperImpl = myMapper,                  // 可选
)
```

**Analytics 单独替换**（与上表等价的最小写法）：

```kotlin
AppMainShell(
    registry = routeRegistry,
    startDestination = MainTab.startDestinationRoute,
    navigator = navigator,
    analyticsImpl = MyProductAnalytics(), // 实现 Analytics
)
```

**性能 Reporter 替换**（推荐经 `AppMainShell`，与 Analytics 对称）：

```kotlin
AppMainShell(
    registry = routeRegistry,
    startDestination = MainTab.startDestinationRoute,
    navigator = navigator,
    performanceImpl = MyApmPerformanceReporter(), // 实现 PerformanceReporter
)
```

**崩溃 Reporter 替换**：

```kotlin
AppMainShell(
    registry = routeRegistry,
    startDestination = MainTab.startDestinationRoute,
    navigator = navigator,
    crashReporterImpl = object : CrashReporter {
        override fun reportCrash(throwable: Throwable, fatal: Boolean) {
            // Bugly / Crashlytics …
        }
    },
)
```

Release 包另在 `TaskFlowApplication.onCreate` 调用 `ReleaseCrashMonitoring.install(this)`，用于 ANR 探测与第三方 SDK 初始化占位（见 §8.2）。

**拦截链替换登录 / 深链 / 权限校验示例**：

```kotlin
CompositionLocalProvider(
    LocalLoginSession provides mySession,
    LocalDeepLinkRouteMapper provides myMapper,
) {
    val chain = rememberAppRouterInterceptorChain(
        loginSession = mySession,
        deepLinkRouteMapper = myMapper,
        permissionGrantChecker = SystemPermissionGrantChecker(context),
    )
    AppNavHost(..., routerInterceptorChain = chain)
}
```

集成宿主在 `BaseScaffold` 内按顺序装配：**性能 → 埋点 → 异常监控（含崩溃 Local + 网络横幅）**；独立调试壳 `FeatureDebugShell` 经同一 `BaseScaffold` 继承行为（Release 三联需集成 `app` 壳才自动切换）。

替换产品埋点：实现 `Analytics`，在壳层传入 `analyticsImpl`，业务仍调用 `logUiInteraction` / `PageLifecycleLog`，零改动。

### 8.2 生产可观测三联（Analytics / Performance / Crash）

**实现位置（app 模块）**：

| 能力 | Debug 实现（component_base） | Release 实现（app） | 统一落盘 / SDK 契约 |
|------|------------------------------|---------------------|---------------------|
| 埋点 | `DebugAnalytics` | `ReleaseAnalytics` | `ReleaseObservabilityContract`，Tag `TaskFlow/Observability` |
| APM | `DebugPerformanceReporter` | `ReleasePerformanceReporter` | 同上，`channel=performance` |
| 崩溃 | `DebugCrashReporter` | `ReleaseCrashReporter` + `ReleaseCrashMonitoring` | 同上，`channel=crash`；含未捕获异常与 ANR |

**数据契约（单行日志，便于 ELK / 自研平台解析）**：

```text
channel=<analytics|performance|crash> event=<事件或指标名> pageId=<页面ID> actionId=<操作ID> params=k=v,...
```

- **Analytics**：`page_enter` / `page_leave` / `page_args_change` / `ui_interaction`（交互的 `actionId` 与 `logUiInteraction` 的 `identifier`、以及 `trackInteraction` 的 `operationId` 一致）。
- **Performance**：`first_frame`、`scroll_fps`、`page_dwell`（`pageId` 与 `PageLifecycleLog.pageName` 对齐）。
- **Crash**：`crash`（`actionId=app_uncaught_crash`）、`anr`（`actionId=app_anr`）；壳级 `pageId=AppShell`。

SDK 接入：修改 `app` 模块 `ReleaseObservabilityContract.dispatchToCompanyPlatform` 与各 `Release*` 实现中的 TODO，**无需改 component_base 或 Feature**。

### 8.3 全局异常监控与网络离线横幅

由 **`ExceptionMonitoringRoot`** 装配（`BaseScaffold` 在**拥有全局宿主**时自动包裹，内层继承父级宿主时不再重复安装）。

| 能力 | 行为 | 业务是否改动 |
|------|------|--------------|
| 未捕获异常 | `ExceptionHandler.installUncaughtExceptionHandler` → `CrashReporter` → 交还系统默认处理器 | 否 |
| 协程可选钩子 | `ExceptionHandler.coroutineExceptionHandler`（不替代 `BaseViewModel.launchTask`） | 按需 |
| 业务异常 Snackbar | `ExceptionHandler.handleBusinessException`（Error 级走崩溃上报） | 按需 |
| 网络断开 | `ConnectivityManager.NetworkCallback` + `NetworkChecker` → 顶部 `NetworkOfflineBanner` | 否 |
| 网络恢复 | 横幅自动隐藏 | 否 |

崩溃上报经 **`LocalCrashReporter`** / **`CrashReporterRegistry`** 与壳层 `crashReporterImpl` 对齐；调试默认同时写 `Logger` 与 Analytics outcome。

### 8.4 本地可观测日志查看（唯一用户入口）

本地 JSONL 由 **`LocalLogStore`**（`component_core`）写入，保留 7 天；**查看、筛选、分页、导出、清空** 全部在集成壳 **底部「日志」Tab**（`feature_log`，路由 `app/log`，`pageId=LogViewer`）完成，Debug / Release 均展示，无独立环境开关。

| 入口 | 状态 | 职责 |
|------|------|------|
| **日志 Tab**（`feature_log`） | **唯一正式入口** | 面向日常联调与 Release 自检：类型筛选、下拉刷新、展开详情、按筛选或全量导出分享、清空 |
| ~~`TaskFlowObservabilityDebugActivity`~~ | **已移除** | 曾与 Tab 能力重复（简易列表 + 导出）；无「可观测配置 / 日志注入 / 性能模拟」等进阶能力，故删除，避免双轨误解 |

深度调试（模拟注入、改 Reporter 实现、SDK 契约验证）在 **壳层 `Release*` / `Debug*` 实现** 与 Logcat（`TaskFlow/Observability`、`PageLifecycle` 等）完成，不另开平行 UI。若未来需要开发者专用面板，应新增独立能力（而非恢复旧 Activity 的重复列表）。

## 9. 全局组件使用约定（component_base）

| 场景 | 组件 / API | 说明 |
|------|------------|------|
| 全局宿主 | `BaseScaffold` | 单 Snackbar 队列、Loading 遮罩、`DialogHost` |
| 一级列表 / Tab | `ListScaffold` | `title` / `actions` / FAB；可折叠顶栏 |
| 二级详情 | `PageScaffold` | 顶栏返回 + `onBackIntercept`（WebView 内后退等） |
| 确认弹窗 | `ConfirmDialog` 或 `showConfirmDialog` | 登录/权限引导等走 DialogController |
| 底部弹窗 | `showBottomSheet` + `BottomSheet` | 任务列表「更多」为样板；圆角/拖拽/动画由基建统一 |
| 列表状态 | `StateBox` + `StateRefreshableListContent` 等 | contentPadding 用 `rememberStateBoxContentPadding`；**日志 Tab** 使用 `StateBox` + 本地列表 |
| 表单键盘 | `rememberImePadding` | 弹窗内 `imePadding` |

顶栏统一使用 [TopBar](component_base/src/main/kotlin/com/example/zhttaskflow/base/ui/TopBar.kt) / `ListScaffold`；历史 `TaskFlowPageTitleBar` 已自源码移除，尺寸见 `UiConstants.TopBarHeight`。

## 10. 二级页返回规范

1. **统一外壳**：二级页使用 `PageScaffold`，`onNavigateUp` 绑定 `navigator.navigateUp()`。
2. **系统返回键**：由 Scaffold 内 `BackHandler` 与顶栏返回共用 `handlePageBack(onNavigateUp, onBackIntercept)`。
3. **可消费后退的容器**（如 WebView）：在 `onBackIntercept` 中若 `webView.canGoBack()` 则 `goBack()` 并返回 `true`；否则返回 `false` 走 `navigateUp`。参考 `ArticleDetailScreen`。
4. **一级 Tab 根页（首个 Tab：资讯列表）**：`ListScaffold(interceptTabRootBackToDesktop = true)`，系统返回退桌面而非销毁进程；任务/日志 Tab 根页为 `false`。

禁止在 Screen 内单独再挂一层 `BackHandler` 与 Scaffold 冲突。

## 11. 交互日志与埋点规范

### 11.1 三类必埋（团队标准）

规范定义于 `ComposeInteractionLogging.kt` KDoc，底层统一走 **`Analytics`**（默认 `DebugAnalytics` → `Logger`）。

| 类型 | API | 要求 |
|------|-----|------|
| 页面曝光 | `PageLifecycleLog(pageName, pageArgs)` | `pageName` 与 Screen 一致（如 `LogViewer`、`TaskList`）；与性能 `pageId` 对齐 |
| 核心 CTA | `logUiInteraction` / `clickWithLog` / `listItemClickWithLog` | 必传 `pageId`（与 `pageName` 对齐）；`actionId` 为入参 `identifier`，命名 `{page}_{控件}` |
| 操作结果 | `logUiOutcome` 或 `logUiInteraction(action = success\|failure\|info, …)` | 在 Snackbar 等反馈处；必传 `pageId`；`params` 可带 `message`，勿记敏感信息 |

**日志格式**（Debug 经 `AnalyticsMessageFormatter`，键名与实现一致）：

```text
action=click pageId=TaskList actionId=task_list_more params=...
```

页面生命周期（`DebugAnalytics` / Tag `PageLifecycle`）：

```text
onEnter page=TaskList args=count=3
onLeave page=TaskList
```

### 11.2 业务扩展埋点（可选）

```kotlin
val analytics = rememberAnalytics()
analytics.trackUiClick(
    operationId = "log_viewer_action", // 日志与 Release 契约中的 actionId
    pageId = "LogViewer",
    params = mapOf("entranceId" to id),
)
```

`trackUiClick` / `trackInteraction` 的入参 **`operationId`** 在 Debug / Release 输出中统一格式化为 **`actionId=`** 字段（与 `logUiInteraction` 的 `identifier` 同义）。

### 11.3 页面性能监控（Performance）

**模块**：`component_base` → `com.example.zhttaskflow.base.performance`。

| 指标 | 含义 | 触发方式 |
|------|------|----------|
| `first_frame` | 自 `beginPage` 到首帧绘制耗时（ms） | `PageLifecycleLog` 进入后，Scaffold 内容区 `withFrameMillis` |
| `scroll_fps` | 用户滚动阶段平均帧率 | Scaffold 内容区 `nestedScroll` 感知滚动 + `Choreographer` 采样 |
| `dwell` | 页面停留时长（ms） | `PageLifecycleLog` 离开 composition 时 `endPage` |

**接入（业务默认零代码）**：

1. 页面使用 `PageLifecycleLog(pageName = …)`（与埋点 `pageName` / `pageId` 一致）。
2. 确保页面在 **`BaseScaffold` 子树**内（集成 `AppMainShell` 或独立 `FeatureDebugShell` 均已装配）。
3. Logcat 过滤 Tag **`PagePerformance`**（Debug）或 **`TaskFlow/Observability`**（Release 契约），示例：`metric=first_frame pageId=TaskList durationMs=42`。

**APM 扩展**：实现 `PerformanceReporter`，经 `AppMainShell(performanceImpl = …)` 注入（§8.1、§8.2）。未注入 `LocalPerformance` 时为 `Performance.NoOp`，不影响功能。

## 12. 路由与拦截链（component_nav）

- 注册：`registerXxxRoutes(RouteRegistry, navigator)`
- 常量：`LogNavRoutes`、`TaskNavRoutes`、`ArticleNavRoutes`
- 默认链：`rememberAppRouterInterceptorChain()`（装配于 `AppMainShell`）
- 调试壳：`FeatureDebugShell` 与 App 壳行为对齐

**执行顺序**（`RouterInterceptor.priority` **数值越大越先**执行，与 `RouterInterceptorPriorities`、源码装配一致）：

| 顺序 | 拦截器 | priority | 说明 |
|------|--------|----------|------|
| 1 | `DeepLinkInterceptor` | **200** | 解析深链 marker / 运营 URI → 内部 path，`Redirect` 后继续链 |
| 2 | `PermissionInterceptor` | **150**（`LOGIN + 50`） | 剥离 `permissionGroup`；**先于**登录执行 |
| 3 | `LoginInterceptor` | **100** | 剥离 `needLogin` |

> 常量 `PERMISSION = 80` 为预留值；运行中权限拦截器使用 **150**，勿与 80 混淆。

失败：`Abort` → 拦截链 UI 桥 → 全局 Snackbar（Error）；与 ViewModel Effect 无关。

### 12.1 深链接入

**运营 URL 格式**（`DeepLinkRouteMapperImpl` 默认）：

```text
taskflow://nav/route?target=<URL 编码的内部 path>
```

示例：

```text
taskflow://nav/route?target=feature_task%2Fdetail%2Fdemo-1
```

**统一 API**：`com.example.zhttaskflow.nav.deeplink.DeepLinkNavigation`（集成壳 `MainActivity`、独立调试 Activity 共用）。

| 方法 | 用途 |
|------|------|
| `extractDeepLinkUri(intent)` | 从 `ACTION_VIEW` 的 `Intent.data` 提取 URI；普通启动为 `null` |
| `enrichExternalDeepLinkUri(uri)` | 对标准 `target` 应用壳层门禁（见下） |
| `prepareNavigationRoute(uri)` | `enrich` + `RouteDeepLinkMarker.wrap`，供 `AppNavigator.navigate` |

**壳工程（`MainActivity`）**：`onCreate` / `onNewIntent` 入队 → NavHost 就绪后：

```kotlin
navigator.navigate(DeepLinkNavigation.prepareNavigationRoute(uri))
```

Manifest 声明 `taskflow` scheme；`launchMode=singleTop` + `onNewIntent`。桌面 `MAIN`/`LAUNCHER` 无 `data`，不受影响。

**壳层门禁 `applyShellRouteGatePolicy`**（委托 [RouteGatePolicy]；与 RouteHost 标记对齐）：

- 任务详情、资讯详情等策略表内 path 自动叠加与内链等价的登录 / 权限标记。
- 完整清单见 **`docs/TASKFLOW_ROUTE_GATES.md`**。
- 其它 path 不改动；运营也可在 `target` 中直接编码已带 `needLogin` / `permissionGroup` query 的 path。

**与内链一致**：`navigate` 后仍走 **深链 200 → 权限 150 → 登录 100**；映射得到的纯净 path 若未带标记，行为与未标记的内链相同。

**adb 验证**：

```bat
adb shell am start -a android.intent.action.VIEW -d "taskflow://nav/route?target=feature_task%%2Fdetail%%2Fdemo-1" com.example.zhttaskflow
```

### 12.2 登录拦截（RouteHost 标记）

ViewModel 只下发纯净 path；RouteHost 跳转前包裹：

```kotlin
navigator.navigate(
    RouteAuthMarker.withNeedLogin(TaskNavRoutes.detailPath(taskId))
)
```

未登录：全局 `DialogController` 引导 → 模拟登录 → Snackbar 成功 → 自动继续跳转。

### 12.3 权限拦截（RouteHost 标记 + 系统运行时）

样板：**任务详情**叠加登录 + 存储权限（`feature_task` / `TaskRoute.kt`）：

```kotlin
navigator.navigate(
    RouteGatePolicy.enrichNavigationPath(effect.url),
)
// 或 feature_task 封装：navigationPathRequireLogin(effect.url)
```

- 标记：`RoutePermissionMarker.withStoragePermission(route)` 或 `withPermissionGroup(route, groupId)`
- 默认校验：`SystemPermissionGrantChecker` + `RequestMultiplePermissions`（有 `ComponentActivity` 时）
- 无 Activity 或显式 `PermissionGrantCheckerMode.Demo` 时降级为 `DemoPermissionGrantChecker`（内存模拟，便于单测 / 特殊调试）
- 未授权：`DialogController` 引导 → 系统授权或模拟 → Snackbar 结果；拒绝则 Snackbar Error

未标记路由 **不经过** 权限拦截，行为与改造前一致。

## 13. 六大能力矩阵（L4 演进）

| 维度 | 等级 | 现状摘要 |
|------|------|----------|
| 组件化与 Clean 分层 | L4 | Feature 垂直三层 + 路由注册；无 feature 互依；domain 无 Android |
| MVI 与 Effect 规范 | L4 | 双 Collector；`ShowSnackbar` + `SnackbarType`；无 Toast |
| 路由与拦截 | L4 | 深链 API 统一；门禁 [RouteGatePolicy] + `TASKFLOW_ROUTE_GATES.md`；权限 150 → 登录 100 |
| UI 平台与脚手架 | L4 | 单宿主；列表双范式；骨架常量收敛 |
| 可观测性（日志/埋点/性能/崩溃） | L4 | 生产三联 + `actionId` 规范；五类壳层注入；全局异常与离线横幅 |
| 工程化与构建 | L3+ | Version Catalog、build-logic、双模式 Feature；`checkDependencyRules` |

**L4 含义**：文档与实现一致、横切能力**五类**可注入、生产可观测三联与权限/深链闭环可演示；产品 SDK 以壳层 `Release*` / 自定义 `*Impl` 替换为主，无需改 Feature 业务代码。

## 14. Feature 双模式

| 模式 | gradle.properties | 说明 |
|------|-------------------|------|
| library | `feature.*.standalone=false` | 集成到 app |
| application | `true` | 独立 Debug Activity + 同一套路由/拦截链 |

## 15. 资源前缀

app_、base_、core_、nav_、task_、article_、log_ 等；Lint `MissingPrefix` 为 error。

## 16. 构建验证

```bat
.\gradlew.bat clean :app:compileDebugKotlin
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintVitalRelease
.\gradlew.bat checkDependencyRules
```

## 17. 新人 Onboarding 速查

1. 新页面：MVI 四件套 + `PageLifecycleLog` + 核心按钮 `logUiInteraction`（**必带 `pageId`**，`identifier` 即日志中的 **`actionId`**）。
2. 新路由：在 `component_nav` 增常量 → Feature `registerXxxRoutes` → RouteHost 消费导航 Effect。
3. 需登录/权限页：仅在 RouteHost `navigate` 前加 Marker，不改 ViewModel path。
4. 二级页：`PageScaffold` + 返回规范；WebView 用 `onBackIntercept`。
5. 弹窗：优先 `showBottomSheet` / `showConfirmDialog`，不自建 `ModalBottomSheet`（除非基建扩展）。
6. 深链：运营 URL → `DeepLinkNavigation.prepareNavigationRoute` → `navigator.navigate`（见 §12.1）。
7. 性能 / 崩溃：保持 `PageLifecycleLog`；Debug 查 `PagePerformance` / `Exception`；Release 查 `TaskFlow/Observability`；APM 与崩溃经 `AppMainShell` 的 `performanceImpl` / `crashReporterImpl`（§8.1–§8.3）。
8. 读源码 KDoc：`BaseArchitecture`、`NavArchitecture`、`MainActivity`、`AppMainShell`、`DeepLinkNavigation`、`CrashReporter`、`ComposeInteractionLogging.kt`。

## 18. 命名规范（YAGNI + 冲突驱动）

### 18.0 P0 应用入口（强制）

- **`TaskFlowApplication`** 必须与 `AndroidManifest.xml` 中 `android:name` **完全一致**，且实现类 **必须与本仓库源码同仓可追溯**（禁止 Manifest 指向缺失类、重命名后未同步 Manifest、或仅存在于历史分支的入口类）。
- 发布前自检：`app/src/main/AndroidManifest.xml` → `com.example.zhttaskflow.TaskFlowApplication` 与 `app/.../TaskFlowApplication.kt` 可互相跳转。

### 18.1 原则

0. **YAGNI 命名**：**仅当当前代码库已存在真实编译/符号冲突**（与 Kotlin 标准库、AndroidX、Material3 等同名无法共存）时，才调整对外主名；真冲突处理顺序为 **定义侧 `import … as …` → `App*` 语义短名 → 最后保留 `TaskFlowApplication` 工程入口**。禁止为「将来可能冲突」提前加 `TaskFlow` 工程前缀。
1. **默认简短命名**：新类型、函数、Compose API 优先使用领域语义或通用短名（`PageScaffold`、`RouteRegistry`、`rememberNavigator`），不为「可能冲突」提前加工程前缀。
2. **真冲突才处理**：仅在与 Kotlin 标准库、AndroidX、Material3、Navigation 等同名或强混淆时调整；处理顺序为：
   - **定义文件内 `import … as …`**（业务模块零负担，调用封装名即可）；
   - 仍无法区分时使用 **`App*` 语义短名**（如 `AppNavHost`、`AppNavigator`、`AppTheme`、`AppIcons`）；
   - **最后**才保留工程前缀——当前仅 **`TaskFlowApplication`** 作为应用入口类强制保留。
3. **禁止夹心混用**：不得出现 `ReleaseTaskFlow*`、`DebugTaskFlow*`、`XxxTaskFlowYyy` 等前后缀拼接形式；环境实现统一 `Debug*` / `Release*`。
4. **命名收尾状态**：YAGNI 短名与 `App*` 冲突处理已落地；**已移除**全部 `TaskFlow*` 命名过渡 `@Deprecated` 别名，仅保留 §18.0 / §18.3 政策类与契约类标识。

### 18.2 当前推荐公开 API（摘录）

| 领域 | 首选名称 | 说明 |
|------|----------|------|
| 应用入口 | `TaskFlowApplication` | **唯一**强制 `TaskFlow` 类名 |
| 导航宿主 | `AppNavHost`、`AppNavigator`、`LocalNavigator` | NavHost / Navigator 与 AndroidX 冲突 |
| 主题 | `AppTheme` | 与 `MaterialTheme` 语义区分 |
| 图标入口 | `AppIcons` | 与 `Icons` 冲突 |
| UI 壳层 | `BaseScaffold`、`ListScaffold`、`PageScaffold`、`StateBox` | 二级页用 `PageScaffold` |
| Material 封装 | `Divider`、`SnackbarHost`、`SnackbarVisuals`、`PullToRefreshBox` | 定义在 `component_base`，内部 M3 别名 |
| 路由 / 拦截 | `NavRoutes`、`RouterInterceptor`、`LoginInterceptor`、`DeepLinkNavigation`、`RouteGatePolicy` | 无工程前缀 |
| MVI 标记 | `NavigationUiEffect`、`PresentationUiEffect`、`UiEffectConsumption` | 双 Collector 规范 |
| 可观测 | `Analytics`、`CrashReporter`、`PerformanceReporter`、`LocalLogStore` | Release dex 校验含 `LocalLogStore` 等类名 |

Compose 横切：使用 `rememberSnackbarDispatcher()`、`LocalNavigator`、`rememberNavigator()` 等短名。

### 18.3 契约字符串（非类型名，慎改）

- Lint **Issue ID**：`TaskFlowNoToastInFeature`、`TaskFlowNoBareScaffoldInFeature`、`TaskFlowLogUiInteractionMissingPageId`（与 CI / `ConfigureAndroidCommon` 一致）。
- 崩溃上报 actionId 等：`TASKFLOW_CRASH_*` 常量。
- Logcat 契约 Tag：`TaskFlow/Observability`；深链 scheme：`taskflow`。
- Gradle 插件 ID / `taskFlow {}` DSL（build-logic）。

### 18.4 内部实现与文件名

- 实现类可无前缀：`SnackbarDispatcher`、`RouteRegistryImpl`、`DeveloperObservability` 等。
- **源码文件名**与对外主类型一致（如 `NavRoutes.kt`、`BaseScaffold.kt`）；仅 `TaskFlowApplication` 与 build-logic `TaskFlowAndroid*Plugin` 保留 `TaskFlow` 文件名。
- build-logic 插件类名（`TaskFlowAndroidLibraryPlugin` 等）属于构建契约，与业务 API 命名无关。

### 18.5 禁止项

- Feature 业务类型名加 `TaskFlow`（如 `ArticleViewModel`、`TaskRoute` 保持领域命名）。
- Feature 层直接 `import androidx.compose.material3.Scaffold` / `Divider` / `SnackbarHost` / `pulltorefresh.PullToRefreshBox`（见自定义 Lint）。
- 为假想冲突预加 `TaskFlow` 前缀的新 API。

### 18.6 命名过渡层（已完成）

- 曾用于平滑迁移的 `DeprecatedApi.kt` / `InfrastructureDeprecatedApi.kt` / `nav/DeprecatedApi.kt` 及分散 `@Deprecated` 转发 **已全部删除**（首次开发无外部接入方，直接收尾）。
- 新代码 **仅** 使用 §18.2 短名与 `App*` API；禁止再引入 `TaskFlow*` 业务类型名（契约字符串除外）。

[BaseArchitecture]: ../component_base/src/main/kotlin/com/example/zhttaskflow/base/doc/BaseArchitecture.kt
[NavArchitecture]: ../component_nav/src/main/kotlin/com/example/zhttaskflow/nav/doc/NavArchitecture.kt
[UiEffectConsumption]: ../component_base/src/main/kotlin/com/example/zhttaskflow/base/ext/UiEffect.kt
