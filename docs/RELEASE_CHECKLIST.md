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
2. 冷启动后按底部 Tab 顺序浏览：**资讯列表 → 任务列表 → 日志**，并进入任务/资讯详情，产生埋点与性能样本。
3. **主路径（与线上用户一致）**：底部 **日志** Tab → 确认列表有 **埋点 / 性能 / 崩溃** 条目；顶栏 **导出分享** 出现系统分享面板与 `.jsonl` 文件。
4. **可选调试页**（与 Tab 日志能力并存，用于崩溃摘要快查）：

   ```bat
   adb shell am start -n com.example.zhttaskflow/.observability.TaskFlowObservabilityDebugActivity
   ```

5. Logcat 过滤 `TaskFlow/Observability`（Release 契约日志，与本地 `files/taskflow_observability/logs/` JSONL 并存）。

> Release 包在 `isTaskFlowDebugLoggingEnabled() == false` 时，`AppMainShell` 默认注入 `Release*` 实现；Debug 安装包仍走 `TaskFlowDebug*` 实现。**日志 Tab 在 Debug / Release 均可见，无环境开关。**

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
| 20 | 触发崩溃或调试页模拟 | 产生 `logType=crash` 记录 | **日志 Tab** 或 `TaskFlowObservabilityDebugActivity` 可见崩溃类条目 / 上次崩溃摘要 | ☐ |
| 21 | 重启 App 后查崩溃 | 打开日志 Tab 或调试页 | `last_crash.jsonl` 摘要仍可读（若曾写入崩溃） | ☐ |
| 22 | 导出崩溃日志 | 日志 Tab「导出分享」 | `.jsonl` 含 `logType=crash` 与 `stackTrace` 字段 | ☐ |

> 切勿在产线用户包常驻 ANR Watchdog 高压测试；仅验证一次即可。

### 3.4 性能指标采集

| # | 场景 | 预期 | 通过 |
|---|------|------|------|
| 23 | 进入任务列表并滚动 | 日志 Tab 或 Logcat 出现 `scroll_fps` / `first_frame` 类性能记录 | ☐ |
| 24 | 慢首帧或低 FPS（可选） | 性能记录中 `anomaly=true`（阈值见 `ReleaseTaskFlowPerformanceReporter`） | ☐ |
| 25 | 离开页面 | 存在 `page_dwell` / 停留类指标 | ☐ |

### 3.5 离线功能

| # | 场景 | 预期 | 通过 |
|---|------|------|------|
| 26 | 飞行模式启动 App | 网络离线横幅展示（若已授权网络状态） | ☐ |
| 27 | 离线浏览已缓存/内存任务数据 | 列表/详情不依赖外网；错误态可重试 | ☐ |
| 28 | 可观测日志写入 | 全程无网络，本地 `taskflow_observability/logs/` 仍可追加；**日志 Tab** 可查看（Release） | ☐ |

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
