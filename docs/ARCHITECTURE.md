# ZhtTaskFlow 架构说明

> 工程级约定与模块边界；UI 平台接入细节见 `component_base` 的 [TaskFlowBaseArchitecture] 与 `component_nav` 的 [TaskFlowNavArchitecture]（源码 KDoc 对象）。

## 1. 目标

多模块 **Feature 垂直 DDD + Clean Architecture**（各 Feature 内嵌 `domain` / `data` / `presentation` / `navigation`）+ **标准 MVI**（`BaseUiState` / `BaseUiEvent` / `BaseUiEffect` / `BaseViewModel`）。壳工程 `app` 仅组装路由与全局宿主。

## 2. 模块职责

| 模块 | namespace（自动） | 职责 |
|------|-------------------|------|
| app | com.example.zhttaskflow | 壳工程、NavHost、拦截链、条件集成 Feature |
| component_base | com.example.zhttaskflow.base | MVI 基类、Compose 脚手架、Snackbar/Loading/Dialog、Inset/IME |
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

## 7. presentation 与 MVI（S3）

| 概念 | 约定 |
|------|------|
| UiState | `BaseUiState<T>`，Screen 用 `StateBox` 四态 |
| UiEvent | 密封类，仅 `ViewModel.onEvent` 入口 |
| UiEffect | 密封类；**导航**与**展示**分标记接口，双 Collector |
| 用户提示 | `ShowSnackbar` + 全局 Snackbar，**不用 Toast** |
| 跨页跳转 | ViewModel 发导航 Effect 或 path；RouteHost 调 `TaskFlowNavigator` |
| 跳转前门禁 | 登录/权限/深链：`TaskFlowRouterInterceptorChain`（非 UiEffect） |

双 Collector 与拦截链区别见 `TaskFlowUiEffectConsumption`、`TaskFlowRouterInterceptor` 文件头 KDoc。

## 8. UI 平台快速索引（component_base）

- 全局宿主：`TaskFlowBaseScaffold`（单 Snackbar 队列）
- 列表页：`TaskFlowListScaffold` + `TaskFlowStateRefreshableListContent` / `TaskFlowStatePaginatedListContent`
- 详情页：`TaskFlowScaffold` + `StateBox` + `PageLifecycleLog`
- 表单 IME：`rememberTaskFlowImePadding`（弹窗示例见任务添加对话框）

## 9. 路由与拦截（component_nav）

- 注册：`registerXxxRoutes(TaskFlowRouteRegistry, navigator)`
- 常量：`TaskFlowHomeNavRoutes`、`TaskFlowTaskNavRoutes`、`TaskFlowArticleNavRoutes`
- 默认链：`rememberTaskFlowAppRouterInterceptorChain()`（深链 → 登录 → 权限）
- 调试壳：`TaskFlowFeatureDebugShell` 与 App 壳行为对齐

## 10. Feature 双模式

| 模式 | gradle.properties | 说明 |
|------|-------------------|------|
| library | `feature.*.standalone=false` | 集成到 app |
| application | `true` | 独立 Debug Activity + 同一套路由/拦截链 |

## 11. 资源前缀

app_、base_、core_、nav_、task_、article_、home_ 等；Lint `MissingPrefix` 为 error。

## 12. 构建验证

```bat
.\gradlew.bat clean :app:compileDebugKotlin
.\gradlew.bat :app:assembleDebug
.\gradlew.bat checkDependencyRules
```

[TaskFlowBaseArchitecture]: ../component_base/src/main/kotlin/com/example/zhttaskflow/base/doc/TaskFlowBaseArchitecture.kt
[TaskFlowNavArchitecture]: ../component_nav/src/main/kotlin/com/example/zhttaskflow/nav/doc/TaskFlowNavArchitecture.kt
