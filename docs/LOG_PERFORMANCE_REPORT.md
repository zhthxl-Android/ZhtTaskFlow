# 日志 Tab 5000 条压测报告

> 归档用途：记录本地可观测日志在大数据量下的性能表现与测试方法。  
> 压测数据脚本位于仓库 `scripts/`，**不打包进 Release APK**。

---

## 1. 测试目标与通过标准

| 维度 | 标准 |
|------|------|
| 数据准备 | `generate_test_logs.ps1` 可灌入 **5000** 条测试日志（埋点 / 性能 / 崩溃各约 1/3） |
| 进入日志页 | 冷启动后进入日志 Tab，**首屏可见列表**耗时 **&lt; 500 ms**（索引已就绪的稳态场景；首次灌数后重建 `.idx` 另计，见 §4.1） |
| 列表滑动 | 快速上下滑动 + 加载更多，无明显卡顿、无 ANR |
| 筛选 | 切换「全部 / 埋点 / 性能 / 崩溃」响应可感知为即时（&lt; 200 ms 量级） |
| 导出 | 导出操作完成时间可接受且无 OOM；产品默认导出上限 **2000 条**（`LOG_EXPORT_MAX_ENTRIES`） |
| 内存 | Profiler 观察无持续爬升、无 OOM；压测后可「清空日志」恢复空态 |
| 清理 | 清空后新埋点可正常写入与展示 |

---

## 2. 测试环境

| 项 | 内容 |
|----|------|
| 工程 | ZhtTaskFlow（组件化 Clean + MVI，`feature_log` + `TaskFlowLocalLogStore`） |
| 存储路径 | `files/taskflow_observability/logs/{yyyy-MM-dd}.jsonl` + 侧车 `{day}.jsonl.idx` |
| 列表分页 | 每页 **50** 条（`LOG_PAGE_SIZE`），按索引倒序，仅解析当前页 JSONL 行 |
| 压测脚本 | `scripts/generate_test_logs.ps1` |
| 报告日期 | 2026-09-02 |
| 执行机（脚本校验） | Windows 10，PowerShell，`GenerateOnly` 本地生成 |
| 真机 / 模拟器 | *待填写：型号、Android 版本、ABI、Debug/Release 包* |
| Android Studio | *待填写：版本；Profiler Memory / CPU* |

### 2.1 自动化脚本校验（本机已执行）

```text
.\scripts\generate_test_logs.ps1 -GenerateOnly -Count 5000

==> Generating 5000 test log records...
    Output: ...\build\log-seed
    Files : ...\2026-09-02.jsonl
    Lines : 5000 (expected 5000)
```

生成记录与 `TaskFlowLocalLogStore.encodeRecord` 字段对齐（`timestamp`、`logType`、`pageId`、`event`、`params`；崩溃含 `stackTrace` / `deviceInfo` / `anomaly=true`）。

### 2.2 设备灌数步骤（Debug 可 `run-as`）

1. 安装 **Debug** 包：`com.example.zhttaskflow`（须 `debuggable`）。
2. 连接设备并确认 `adb devices` 可见。
3. 可选清空旧数据：  
   `.\scripts\generate_test_logs.ps1 -Count 5000 -ClearExisting`
4. 仅生成种子（无设备）：  
   `.\scripts\generate_test_logs.ps1 -GenerateOnly -Count 5000`
5. 冷启动 App → 打开 **日志** Tab（首次可能重建 `.idx`）→ 按 §3 采数。
6. 压测结束：**清空日志**（或 `-ClearExisting` 再测）。

---

## 3. 指标采集方法

### 3.1 进入日志页耗时

- **方式 A**：Android Studio **App Startup / System Trace**，从点击底部「日志」到首屏 `LazyColumn` 首屏 item 绘制完成。
- **方式 B**：在 `LogViewModel` 加载路径临时打 `SystemClock.elapsedRealtime()` 差值（仅本地调试，勿提交）。
- **建议**：区分 **首次打开（索引重建）** 与 **第二次进入（索引已存在）**；通过标准以 **稳态第二次** &lt; 500 ms 为主。

### 3.2 列表滑动 / FPS

- 开启 **Profile GPU Rendering** 或 **Android Studio Profiler → CPU / Frame Timeline**。
- 操作：日志 Tab 内快速 fling 10～20 次；触发「加载更多」直至 `hasMore = false`（5000 条约 100 页）。
- 记录：掉帧次数、平均帧时间、是否出现 jank 峰值。

### 3.3 筛选响应时间

- 快速循环：全部 → 埋点 → 性能 → 崩溃 → 全部（每类 ≥ 3 次）。
- 用秒表或 Systrace 标记从 Chip 点击到列表内容稳定替换的时间。

### 3.4 导出耗时

- 菜单 **导出分享** → **导出全部日志**（或当前筛选）。
- 记录从点击到分享 Sheet 出现 / 文件生成完成的耗时。
- **说明**：领域层与 `ExportLogsUseCase` 默认 **`maxEntries = 2000`**，导出文件行数 ≤ 2000，与 5000 条仓内总量无关；全量 5000 条导出需产品改上限后另测。

### 3.5 内存峰值与 Profiler 快照

- Profiler **Memory**：进入日志 Tab 前拍 baseline → 滑动 + 加载全部页 → 导出 → 再拍 heap dump。
- 关注：`loadFilteredIndexEntries` 持有的索引列表（约 5000 行 × 每行数百字节量级）、`LogRecord` 当前页仅 50 条，不应线性增长到 OOM。
- 保存 **Heap Dump** 文件路径于下表「备注」。

---

## 4. 指标结果

> 下列「实测」栏请在真机完成 §3 后填写；当前仓库 CI/Agent 环境无可用 `adb`，仅完成 §2.1 脚本与架构评估。

| 指标 | 目标 | 实测 | 备注 |
|------|------|------|------|
| 灌入条数 | 5000 | **5000**（GenerateOnly 校验） | 单文件 `build/log-seed/yyyy-MM-dd.jsonl` |
| 进入日志 Tab（稳态） | &lt; 500 ms | *待填* | 索引已存在 |
| 首次打开（重建 `.idx`） | 记录参考值 | *待填* | 一次性 IO，可能 &gt; 500 ms |
| 滑动 FPS / 掉帧 | 流畅 | *待填* | |
| 筛选切换 | &lt; 200 ms 体感 | *待填* | |
| 导出（≤2000 条） | 无 ANR/OOM | *待填* | 秒级内为合理预期 |
| 内存峰值（Java/Kotlin Heap） | 无持续增长 | *待填* | Profiler |
| 清空日志 | 列表空态、可再写入 | *待填* | |

### 4.1 架构侧预期（代码审阅，非替代真机）

- **查询**：`queryPaged` 先 `loadFilteredIndexEntries`（读 `.idx`、过滤、排序），再仅对当前页做 `readRecordAt`；首屏只解析 **50** 条 JSON，符合大数据列表设计。
- **5000 条索引**：全量载入内存约数 MB 级，在中端机上排序 5000 个 long 一般 **&lt; 50 ms**；与 JSON 解析解耦，利于稳态进页 &lt; 500 ms。
- **风险点**：首次缺失 `.idx` 时 `ensureIndexForLogFile` 全文件扫描 JSONL（5000 行）——仅第一次；脚本部署时已 **删除对应 `.idx`** 以模拟真实灌数，需在报告中单独记录该次耗时。
- **导出**：最多读 2000 条记录并写 cache 目录，内存峰值与 2000 条 `LogRecord` 成正比，远低于 5000 条全量驻留。

---

## 5. 结论

| 结论项 | 状态 |
|--------|------|
| 压测脚本可生成指定数量日志 | **通过**（5000 行校验） |
| 脚本不进 Release 包 | **通过**（仅 `scripts/`，无 `app` assets 引用） |
| 5000 条场景流畅、无 OOM | **待真机确认**（架构评估为可接受；以 §4 表实测为准） |
| 清空不影响后续使用 | **待真机确认**（`TaskFlowLocalLogStore.clearAllLogs` + UI 清空链路已有） |

**后续动作**

1. 在目标发布机上执行 §2.2 + §3，补全 §4「实测」列。
2. 若稳态进页 ≥ 500 ms 或筛选明显卡顿：优先考虑索引层优化（例如按天懒加载、避免每次全量 `sortedByDescending`），改后 **重新灌 5000 条** 并更新本报告版本号。
3. 发布前可对照 [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) §3.6 做人工勾选。

---

## 6. 附录：日志类型分布（脚本逻辑）

| `logType` | 约占比 | `event` 前缀 | 说明 |
|-----------|--------|--------------|------|
| `analytics` | 33% | `stress_analytics_*` | 埋点 |
| `performance` | 33% | `stress_perf_*` | 含 `params.metric=scroll_fps` |
| `crash` | 33% | `stress_crash_*` | `anomaly=true`，含合成 `stackTrace` |

索引 `i` 满足 `i % 3` 轮换三类；时间戳递减 1 s/条，便于倒序展示。
