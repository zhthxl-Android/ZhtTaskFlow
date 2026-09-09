# 发布前校验清单

> 自动化门禁见 [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) 与本地脚本 `scripts/ci-verify.ps1`（Windows）/ `scripts/ci-verify.sh`（Unix）。  
> 开发环境说明见 [DEVELOPMENT.md](DEVELOPMENT.md)。

## 一、强制自动化校验（CI / 本地必过）

与 CI 流水线完全一致，**发布前必须全部成功**。不依赖外部 SaaS，仅使用 Gradle Wrapper 与仓库内脚本。

### Windows（PowerShell，仓库根目录）

```powershell
.\scripts\ci-verify.ps1
```

脚本依次执行：`checkDependencyRules` → `clean :app:compileDebugKotlin` → `:app:lintVitalRelease` → 四个模块 `testDebugUnitTest`（并行）→ `:app:assembleRelease` → `verify-release-observability.ps1`。

或分步执行（与 `ci-verify.ps1` / CI `verify` Job 相同）：

```bat
.\gradlew.bat checkDependencyRules --no-daemon
.\gradlew.bat clean :app:compileDebugKotlin --no-daemon
.\gradlew.bat :app:lintVitalRelease --no-daemon
.\gradlew.bat :component_nav:testDebugUnitTest :feature_article:testDebugUnitTest :feature_task:testDebugUnitTest :feature_log:testDebugUnitTest --no-daemon --parallel
.\gradlew.bat :app:assembleRelease --no-daemon
powershell -ExecutionPolicy Bypass -File .\scripts\verify-release-observability.ps1
```

（若策略限制脚本，最后一行可改为在 PowerShell 中 `cd` 到仓库根后执行 `.\scripts\verify-release-observability.ps1`。）

### Linux / macOS / Git Bash

```bash
bash scripts/ci-verify.sh
```

### 分步说明

| 步骤 | 命令（Windows） | 目的 |
|------|-----------------|------|
| 0 | `.\gradlew.bat checkDependencyRules --no-daemon` | 组件化模块依赖红线（CI 独立 Job，本地脚本首步已包含） |
| 1 | `.\gradlew.bat clean :app:compileDebugKotlin --no-daemon` | 全量编译，类型与依赖正确 |
| 2 | `.\gradlew.bat :app:lintVitalRelease --no-daemon` | Release 关键 Lint；自定义三条规则为 **ERROR**，违规阻断 |
| 3 | `.\gradlew.bat :component_nav:testDebugUnitTest :feature_article:testDebugUnitTest :feature_task:testDebugUnitTest :feature_log:testDebugUnitTest --no-daemon --parallel` | 路由门禁、深链与 Feature 核心 JVM 单测 |
| 4 | `.\gradlew.bat :app:assembleRelease --no-daemon` + `verify-release-observability.ps1` | Release 包内含生产可观测实现类 |

**Windows 单测仅跑某一模块时**（排障用，不替代步骤 3 全量）：

```bat
.\gradlew.bat :feature_log:testDebugUnitTest --no-daemon
```

Release 产物校验会检查 APK 的 `classes.dex` 是否包含：`ReleaseAnalytics`、`ReleasePerformanceReporter`、`ReleaseCrashReporter`、`LocalLogStore`。

### 可选：仪表化 E2E（本地，未进 CI）

连接真机或模拟器且允许安装测试 APK 后：

```bat
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
```

覆盖 `app/src/androidTest` 中导航与日志 Tab 冒烟；失败时查看 `app\build\reports\androidTests\connected\debug\index.html`。

---

## 二、Release 可观测与本地日志（建议）

1. 安装 **Release** APK（`app/build/outputs/apk/release/`）。
2. 冷启动后按底部 Tab 顺序浏览：**资讯列表 → 任务列表 → 日志**，并进入任务/资讯详情，产生埋点与性能样本。
3. **主路径（与线上用户一致）**：底部 **日志** Tab → 确认列表有 **埋点 / 性能 / 崩溃** 条目；顶栏 **导出分享** 可选择「当前筛选」或「全部」并出现系统分享面板与 `.jsonl` 文件。
4. Logcat 过滤 `TaskFlow/Observability`（Release 契约日志，与本地 `files/taskflow_observability/logs/` JSONL 并存）。

> Release 包在 `isDebugLoggingEnabled() == false` 时，`AppMainShell` 默认注入 `Release*` 实现；Debug 安装包仍走 `Debug*` 实现。**日志 Tab 在 Debug / Release 均可见，无环境开关。** 历史上独立的 `TaskFlowObservabilityDebugActivity` 已移除，请勿再使用 adb 启动该 Activity。

---

## 三、手动冒烟清单

在 **Release 或候选包** 上逐项勾选（建议测试机清数据后执行）。

### 3.1 核心功能与 Tab（含 S7 日志模块）

**底部 Tab 顺序（固定）**：**资讯列表**（第一个）→ **任务列表** → **日志查看**（最后一个）。  
**默认启动页**：冷启动 `MAIN`/`LAUNCHER` 无深链时，应落在 **资讯列表**（`feature_article/list`），而非任务或日志。

| # | 场景 | 操作步骤 | 预期 | 通过 |
|---|------|----------|------|------|
| 1 | 默认启动页 | 清数据后桌面图标启动 App | 首屏为 **资讯列表**；底栏选中「资讯」；无崩溃 | ☐ |
| 2 | Tab 顺序与切换 | 依次点底栏：资讯 → 任务 → 日志 → 再回到资讯 | 三 Tab 均展示；路由分别为 `feature_article/list`、`feature_task/list`、`app/log`；切换无白屏/重复栈异常 | ☐ |
| 3 | 资讯 Tab 根返回 | 在 **资讯列表**（首个 Tab 根页）按系统返回 | **退到桌面**（`moveTaskToBack`），进程不杀；再次进入恢复资讯 Tab | ☐ |
| 4 | 任务 / 日志 Tab 返回 | 在任务列表或日志 Tab 根页按系统返回 | **不**强制退桌面；可切 Tab 离开 | ☐ |
| 5 | 资讯列表 → 详情 | 点一条资讯 | 进入 WebView 详情；StateBox 正常 | ☐ |
| 6 | 资讯详情返回 | 顶栏返回 / 系统返回（有 H5 历史时） | 先退 WebView 历史，再回资讯列表 | ☐ |
| 7 | 任务列表 → 详情 | 点一条任务 | 详情 StateBox 成功；展示状态/附件等 | ☐ |
| 8 | 任务详情返回 | 顶栏返回 | 回任务列表；无 Toast；Snackbar 仅业务触发时出现 | ☐ |
| 9 | **日志：列表加载** | 进入 **日志** Tab | StateBox 加载后展示条目（或空态「暂无日志」）；默认 **时间倒序** | ☐ |
| 10 | **日志：类型筛选** | 切换 全部 / 埋点 / 性能 / 崩溃 | 列表仅显示对应 `logType`；切回「全部」恢复 | ☐ |
| 11 | **日志：详情展开** | 点击一条日志卡片 | 展开显示完整 JSON 详情；再次点击收起 | ☐ |
| 12 | **日志：导出分享** | 顶栏「导出分享」 | 系统分享面板；附件为 UTF-8 `.jsonl`；内容与本地仓一致（抽样核对 `pageId`/`event`） | ☐ |
| 13 | **日志：清空** | 顶栏「清空日志」→ 二次确认 → 确定 | 列表变空；再次产生埋点后列表可重新出现 | ☐ |
| 14 | Release 落盘 | Release 包浏览各 Tab 后打开 **日志** Tab | 可见 `analytics` / `performance` 等条目；飞行模式下仍可追加（见 §3.5 #28） | ☐ |

### 3.2 深链与路由门禁

| # | 场景 | 操作步骤 | 预期 | 通过 |
|---|------|----------|------|------|
| 15 | 未登录深链 → 任务详情 | 未登录状态下执行任务详情深链（见下） | 登录引导 Dialog → 模拟登录 → **存储权限**引导 → 进入任务详情 | ☐ |
| 16 | 未登录深链 → 资讯详情 | 未登录状态下执行资讯详情深链（见下） | 登录引导 → 进入资讯 WebView 详情（**无**存储权限门禁） | ☐ |
| 17 | 已登录+已授权任务详情 | 登录并授权存储后执行任务详情深链 | 直达详情，与列表进入一致 | ☐ |
| 18 | 深链 → 日志 Tab（可选） | `target=app%2Flog`（见下） | 进入日志查看页；底栏高亮「日志」；无登录拦截 | ☐ |
| 19 | 无效深链 Host | `taskflow://invalid/route?target=...` | Snackbar 错误提示，不崩溃 | ☐ |

深链示例（任务 `demo-1`）：

```bat
adb shell am start -a android.intent.action.VIEW -d "taskflow://nav/route?target=feature_task%%2Fdetail%%2Fdemo-1" com.example.zhttaskflow
```

资讯详情（示例 `articleId=demo-1-1`，`detailUrl` 需 URL 编码）：

```bat
adb shell am start -a android.intent.action.VIEW -d "taskflow://nav/route?target=feature_article%%2Fdetail%%2Fdemo-1-1%%2Fhttps%%3A%%2F%%2Fexample.com" com.example.zhttaskflow
```

日志 Tab：

```bat
adb shell am start -a android.intent.action.VIEW -d "taskflow://nav/route?target=app%%2Flog" com.example.zhttaskflow
```

门禁表见 [TASKFLOW_ROUTE_GATES.md](TASKFLOW_ROUTE_GATES.md)。

### 3.3 崩溃与 ANR 日志写入

| # | 场景 | 预期 | 通过 |
|---|------|------|------|
| 20 | 触发崩溃或模拟 | 产生 `logType=crash` 记录 | **日志 Tab** 筛选「崩溃」可见条目；详情含 `stackTrace` | ☐ |
| 21 | 重启 App 后查崩溃 | 打开日志 Tab | `last_crash.jsonl` 对应记录仍可通过列表/导出查看（若曾写入崩溃） | ☐ |
| 22 | 导出崩溃日志 | 日志 Tab「导出分享」 | `.jsonl` 含 `logType=crash` 与 `stackTrace` 字段 | ☐ |

> 切勿在产线用户包常驻 ANR Watchdog 高压测试；仅验证一次即可。

### 3.4 性能指标采集

| # | 场景 | 预期 | 通过 |
|---|------|------|------|
| 23 | 进入任务列表并滚动 | 日志 Tab 或 Logcat 出现 `scroll_fps` / `first_frame` 类性能记录 | ☐ |
| 24 | 慢首帧或低 FPS（可选） | 性能记录中 `anomaly=true`（阈值见 `ReleasePerformanceReporter`） | ☐ |
| 25 | 离开页面 | 存在 `page_dwell` / 停留类指标 | ☐ |

### 3.5 离线功能

| # | 场景 | 预期 | 通过 |
|---|------|------|------|
| 26 | 飞行模式启动 App | 网络离线横幅展示（若已授权网络状态） | ☐ |
| 27 | 离线浏览已缓存/内存任务数据 | 列表/详情不依赖外网；错误态可重试 | ☐ |
| 28 | 可观测日志写入 | 全程无网络，本地 `taskflow_observability/logs/` 仍可追加；**日志 Tab** 可查看（Release） | ☐ |

### 3.6 日志 Tab 压测与大数据量（建议，Release 或 Debug 均可）

在 **非产线常驻** 前提下，用于验证分页、筛选与导出在条目较多时仍可用（与 `LocalLogStore` 索引分页、`ExportLogsUseCase` 默认 `maxEntries = 2000` 一致）。

| # | 场景 | 操作步骤 | 预期 | 通过 |
|---|------|----------|------|------|
| 29 | 积累样本 | 连续切换三 Tab、进入详情、下拉刷新资讯/任务、重复 3～5 分钟 | 日志 Tab 条目数明显增加（建议 **≥ 50** 条） | ☐ |
| 30 | 列表滚动与加载更多 | 在日志 Tab 快速上下滑动；若出现「加载更多」则触发至无更多 | 无 ANR、无崩溃；滚动流畅；`hasMore=false` 后不再重复请求 | ☐ |
| 31 | 筛选切换压测 | 快速连续切换 全部 → 埋点 → 性能 → 崩溃 → 全部（每类至少 3 次） | 列表与 Chip 选中态一致；无错序或空列表闪屏 | ☐ |
| 32 | 下拉刷新 | 日志 Tab 下拉刷新 | 列表重载成功；筛选条件保持不变 | ☐ |
| 33 | 导出上限与格式 | 「导出分享」→「导出全部日志」 | 生成 UTF-8 `.jsonl`；行数 **≤ 2000**（领域默认上限）；每行可解析为 JSON；抽样字段含 `logType`、`event` | ☐ |
| 34 | 清空后恢复 | 压测后执行「清空日志」并确认，再浏览 App 产生新埋点 | 列表从空态恢复；新条目可筛选、可展开详情 | ☐ |

可选自动化：本地执行 `.\gradlew.bat :app:connectedDebugAndroidTest`（需设备），覆盖导航与日志核心路径；**不替代**上表大数据量手动压测。

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
- [DEVELOPMENT.md](DEVELOPMENT.md) — Windows 构建、CI 命令与单元测试分步  
- [TASKFLOW_ROUTE_GATES.md](TASKFLOW_ROUTE_GATES.md) — 门禁与深链  
- [TASKFLOW_CUSTOM_LINT.md](TASKFLOW_CUSTOM_LINT.md) — 自定义 Lint **ERROR** 与本地校验  
