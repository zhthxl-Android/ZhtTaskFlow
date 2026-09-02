# 发布前校验清单

> 自动化门禁见 [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) 与本地脚本 `scripts/ci-verify.ps1`（Windows）/ `scripts/ci-verify.sh`（Unix）。  
> 开发环境说明见 [DEVELOPMENT.md](DEVELOPMENT.md)。

## 一、强制自动化校验（CI / 本地必过）

与 CI 流水线完全一致，**发布前必须全部成功**。不依赖外部 SaaS，仅使用 Gradle Wrapper 与仓库内脚本。

### Windows（PowerShell，仓库根目录）

```powershell
.\scripts\ci-verify.ps1
```

或分步执行（与 CI 相同任务名）：

```bat
.\gradlew.bat clean :app:compileDebugKotlin
.\gradlew.bat :app:lintVitalRelease
.\gradlew.bat :component_nav:testDebugUnitTest
.\gradlew.bat :app:assembleRelease
.\scripts\verify-release-observability.ps1
```

（若策略限制脚本，可用 `powershell -ExecutionPolicy Bypass -File .\scripts\verify-release-observability.ps1`。）

### Linux / macOS / Git Bash

```bash
bash scripts/ci-verify.sh
```

### 分步说明

| 步骤 | 命令（Windows） | 目的 |
|------|-----------------|------|
| 1 | `.\gradlew.bat clean :app:compileDebugKotlin` | 全量编译，类型与依赖正确 |
| 2 | `.\gradlew.bat :app:lintVitalRelease` | Release 关键 Lint，违规阻断 |
| 3 | `.\gradlew.bat :component_nav:testDebugUnitTest` | 路由门禁与深链核心单测 |
| 4 | `.\gradlew.bat :app:assembleRelease` + `verify-release-observability.*` | Release 包内含生产可观测实现类 |

Release 产物校验会检查 APK 的 `classes.dex` 是否包含：`ReleaseTaskFlowAnalytics`、`ReleaseTaskFlowPerformanceReporter`、`ReleaseTaskFlowCrashReporter`、`TaskFlowLocalLogStore`。

---

## 二、Release 可观测与本地日志（建议）

1. 安装 **Release** APK（`app/build/outputs/apk/release/`）。
2. 冷启动后浏览首页 → 任务列表 → 任务详情，产生埋点与性能样本。
3. 打开本地日志调试页（无需第三方 SDK）：

   ```bat
   adb shell am start -n com.example.zhttaskflow/.observability.TaskFlowObservabilityDebugActivity
   ```

4. 确认列表中有 **埋点 / 性能** 条目；可点 **导出并分享日志** 验证 JSON Lines 文件。
5. Logcat 过滤 `TaskFlow/Observability`（Release 契约日志，与本地文件并存）。

> Release 包在 `isTaskFlowDebugLoggingEnabled() == false` 时，`AppMainShell` 默认注入上述 `Release*` 实现；Debug 安装包仍走 `TaskFlowDebug*` 实现。

---

## 三、手动冒烟清单

在 **Release 或候选包** 上逐项勾选（建议测试机清数据后执行）。

### 3.1 核心页面跳转

| # | 场景 | 预期 | 通过 |
|---|------|------|------|
| 1 | 启动 App → 首页 Tab | 首页入口卡片展示，无崩溃 | ☐ |
| 2 | 首页 → 任务列表 / 资讯列表入口 | 进入对应 Tab 列表 | ☐ |
| 3 | 底部 Tab：首页 / 任务 / 资讯 | Tab 切换正常，返回栈合理 | ☐ |
| 4 | 任务列表 → 任务详情 | 详情加载 StateBox，展示信息/状态/附件 | ☐ |
| 5 | 资讯列表 → 资讯详情（WebView） | H5 加载；顶栏/系统返回先退 WebView 历史 | ☐ |
| 6 | 详情页顶栏返回 | 回到列表，无重复弹窗/Toast | ☐ |

### 3.2 深链与路由门禁

| # | 场景 | 预期 | 通过 |
|---|------|------|------|
| 7 | 未登录深链进任务详情 | 登录引导 → 模拟登录 → 存储权限引导 → 进入详情 | ☐ |
| 8 | 未登录深链进资讯详情 | 登录引导 → 进入详情 | ☐ |
| 9 | 已登录+已授权深链任务详情 | 直达详情，与列表进入一致 | ☐ |
| 10 | 无效深链 Host | Snackbar 错误提示，不崩溃 | ☐ |

深链示例（任务 `demo-1`）：

```bat
adb shell am start -a android.intent.action.VIEW -d "taskflow://nav/route?target=feature_task%%2Fdetail%%2Fdemo-1" com.example.zhttaskflow
```

门禁表见 [TASKFLOW_ROUTE_GATES.md](TASKFLOW_ROUTE_GATES.md)。

### 3.3 崩溃与 ANR 日志写入

| # | 场景 | 预期 | 通过 |
|---|------|------|------|
| 11 | 触发非致命业务异常（若有调试入口）或模拟崩溃 | `TaskFlowObservabilityDebugActivity` 显示 **上次崩溃** 摘要 | ☐ |
| 12 | 重启 App 后打开调试页 | 仍可读到上次崩溃记录（`last_crash.jsonl`） | ☐ |
| 13 | 导出日志 | 分享面板出现 `.jsonl`，含 `logType=crash` 与 `stackTrace` | ☐ |

> 切勿在产线用户包常驻 ANR Watchdog 高压测试；仅验证一次即可。

### 3.4 性能指标采集

| # | 场景 | 预期 | 通过 |
|---|------|------|------|
| 14 | 进入任务列表并滚动 | 本地日志或 Logcat 出现 `scroll_fps` / `first_frame` 类性能记录 | ☐ |
| 15 | 慢首帧或低 FPS 场景（可选） | 性能记录中 `anomaly=true`（阈值见 `ReleaseTaskFlowPerformanceReporter`） | ☐ |
| 16 | 离开页面 | 存在 `page_dwell` / 停留类指标 | ☐ |

### 3.5 离线功能

| # | 场景 | 预期 | 通过 |
|---|------|------|------|
| 17 | 飞行模式启动 App | 网络离线横幅展示（若已授权网络状态） | ☐ |
| 18 | 离线浏览已缓存/内存任务数据 | 列表/详情不依赖外网；错误态可重试 | ☐ |
| 19 | 可观测日志写入 | 全程无网络，本地 `taskflow_observability/logs/` 仍可追加（Release） | ☐ |

---

## 四、发布签字（可选）

| 角色 | 姓名 | 日期 | 备注 |
|------|------|------|------|
| 开发 | | | CI + 冒烟完成 |
| 测试 | | | Release 包版本号： |
| 负责人 | | | |

---

## 五、相关文档

- [ARCHITECTURE.md](ARCHITECTURE.md) — §8 可观测三联与五类注入  
- [DEVELOPMENT.md](DEVELOPMENT.md) — Windows 构建与 `clean` 文件锁  
- [TASKFLOW_ROUTE_GATES.md](TASKFLOW_ROUTE_GATES.md) — 门禁与深链  
