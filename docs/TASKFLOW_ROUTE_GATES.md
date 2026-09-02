# TaskFlow 业务路由门禁说明

> 实现：`component_nav` → `TaskFlowRouteGatePolicy`；拦截链见 [ARCHITECTURE.md §12](ARCHITECTURE.md) 与 `TaskFlowNavArchitecture`。

## 约定

- ViewModel 下发 **纯净** path（不含 `needLogin` / `permissionGroup`）。
- Feature **RouteHost** 在 `TaskFlowNavigator.navigate` 前调用 `TaskFlowRouteGatePolicy.enrichNavigationPath(path)`（或 Feature 内等价封装）。
- 外部深链 `taskflow://nav/route?target=...` 经 `TaskFlowDeepLinkNavigation.enrichExternalDeepLinkUri` 对 `target` 施加 **相同** 策略后再进入拦截链。
- 未列入下表的路由 **不叠加** 标记，行为与改造前一致。

## 路由门禁表

| 路由 path（纯净） | 模块 | 登录 `needLogin` | 权限 `permissionGroup` | RouteHost / 入口 |
|-------------------|------|------------------|-------------------------|------------------|
| `app/log` | feature_log | — | — | 日志 Tab（无门禁） |
| `feature_task/list` | feature_task | — | — | 任务 Tab |
| `feature_task/detail/{taskId}` | feature_task | ✅ | `storage` | 列表 → 详情；深链 `target` 同 path |
| `feature_article/list` | feature_article | — | — | 资讯 Tab |
| `feature_article/detail/{articleId}/{detailUrl}` | feature_article | ✅ | — | 列表 → WebView 详情 |

## 拦截链顺序（标记剥离后导航）

深链 **200** → 权限 **150** → 登录 **100**。失败时全局 Snackbar（Error），与 MVI 展示 Effect 无关。

## 扩展新页面

1. 在 `TaskFlowRouteGatePolicy.enrichNavigationPath` 增加 path 判定与标记组合。
2. 在本表追加一行说明。
3. 对应 Feature `*Route.kt` 的导航 Effect 消费处调用 `enrichNavigationPath`（或 Feature 封装函数）。
4. 若需深链直达，无需改 `MainActivity`：enrich 已统一走 `TaskFlowRouteGatePolicy`。

## 手动验证

| 场景 | 预期 |
|------|------|
| 未登录进任务详情 | 登录引导弹窗 → 模拟登录 → 继续 → 存储权限引导 |
| 未登录进资讯详情 | 登录引导 → 进入详情 |
| 列表 / 首页 / Tab 切换 | 无拦截 |
| 深链任务详情 | 与列表点进详情相同门禁 |

```bat
adb shell am start -a android.intent.action.VIEW -d "taskflow://nav/route?target=feature_task%%2Fdetail%%2Fdemo-1" com.example.zhttaskflow
```
