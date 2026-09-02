# TaskFlow 自定义 Lint 规则

模块：`component_lint`（经 `lintChecks` 注入 **app** 与 **feature_*** 模块）。

| Issue ID | 说明 | 严重级别 | 构建影响 |
|----------|------|----------|----------|
| `TaskFlowNoToastInFeature` | Feature 层禁止 `android.widget.Toast` | **ERROR** | `lintDebug` / `lintRelease` / `lintVitalRelease` 均阻断（`abortOnError = true`） |
| `TaskFlowNoBareScaffoldInFeature` | Feature 层禁止裸 `Scaffold` | **ERROR** | 同上 |
| `TaskFlowLogUiInteractionMissingPageId` | `logUiInteraction` 必须显式传 `pageId` | **ERROR** | 同上 |

规则在检测器内声明为 `Severity.ERROR`（见 `TaskFlowIssueRegistry`）。根工程 `ConfigureAndroidCommon` 将上述 ID 同步加入 `lint.error` 与 `lint.fatal`，因此：

- **Debug 变体**：执行 `:feature_*:lintDebug` 或 `:app:lintDebug` 时，违规即失败构建。
- **Release 变体**：`lintVitalRelease` 仅检查 Release 关键路径，自定义三条规则同样在 fatal 集合内，**发布流水线必过**。

## 修复建议摘要

1. **Toast** → MVI `UiEffect` + `TaskFlowBaseScaffold` / `TaskFlowSnackbarHost`。
2. **Scaffold** → `TaskFlowListScaffold`（Tab 列表）或 `TaskFlowScaffold`（二级页）。
3. **pageId** → 与 `PageLifecycleLog` 的 `pageName` 使用同一页面常量，例如 `pageId = LOG_PAGE_ID`（日志页）或 `TASK_LIST_PAGE_ID`。

## 本地校验

Windows（仓库根目录）：

```bat
.\gradlew.bat :component_lint:test
.\gradlew.bat :feature_log:lintDebug
.\gradlew.bat :app:lintVitalRelease
```

Linux / macOS：

```bash
./gradlew :component_lint:test
./gradlew :feature_log:lintDebug
./gradlew :app:lintVitalRelease
```

`component_lint:test` 覆盖三条规则的 Detector 单测；Feature 模块日常可跑 `:feature_<name>:lintDebug`；发布前以 `:app:lintVitalRelease` 与 CI 对齐。
