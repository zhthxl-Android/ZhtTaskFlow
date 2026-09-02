# TaskFlow 自定义 Lint 规则

模块：`component_lint`（经 `lintChecks` 注入 **app** 与 **feature_*** 模块）。

| Issue ID | 说明 | Debug | Release（lintVital） |
|----------|------|-------|----------------------|
| `TaskFlowNoToastInFeature` | Feature 层禁止 `android.widget.Toast` | 警告 | **阻断** |
| `TaskFlowNoBareScaffoldInFeature` | Feature 层禁止裸 `Scaffold` | 警告 | **阻断** |
| `TaskFlowLogUiInteractionMissingPageId` | `logUiInteraction` 必须显式传 `pageId` | 警告 | **阻断** |

## 修复建议摘要

1. **Toast** → MVI `UiEffect` + `TaskFlowBaseScaffold` / `TaskFlowSnackbarHost`。
2. **Scaffold** → `TaskFlowListScaffold`（Tab 列表）或 `TaskFlowScaffold`（二级页）。
3. **pageId** → 与 `PageLifecycleLog` 的 `pageName` 使用同一页面常量，例如 `pageId = HOME_PAGE_ID`。

## 本地校验

```bash
./gradlew.bat :component_lint:test
./gradlew.bat :feature_home:lintDebug
./gradlew.bat :app:lintVitalRelease
```

规则默认 `Severity.WARNING`；根工程在 `ConfigureAndroidCommon` 中将上述 ID 加入 `lint.fatal`，由 `lintVitalRelease` 在 Release 路径阻断。
