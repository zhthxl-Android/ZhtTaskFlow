# 开发环境与构建说明

> 面向日常开发与排障；架构与模块约定见 [ARCHITECTURE.md](ARCHITECTURE.md)。

## 平台与工具

| 项 | 约定 |
|----|------|
| JDK | 17（与 `ConfigureAndroidCommon` 一致） |
| Android Studio | 推荐最新稳定版；Windows 10/11 均可 |
| Gradle 入口 | 仓库根目录执行 Wrapper，勿混用系统全局 Gradle |

---

## Windows 与 Linux / macOS 命令对照

本仓库根目录提供 **`gradlew`（Unix）** 与 **`gradlew.bat`（Windows）**。除启动脚本外，**任务名与参数完全相同**。

| 场景 | Linux / macOS | Windows（CMD 或 PowerShell） |
|------|----------------|--------------------------------|
| 编译 Debug Kotlin（集成壳） | `./gradlew :app:compileDebugKotlin` | `.\gradlew.bat :app:compileDebugKotlin` |
| 打 Debug 包 | `./gradlew :app:assembleDebug` | `.\gradlew.bat :app:assembleDebug` |
| 全量 clean 后编译 | `./gradlew clean :app:compileDebugKotlin` | `.\gradlew.bat clean :app:compileDebugKotlin` |
| 单 Feature 独立调试 AAR | `./gradlew :feature_task:assembleDebug` | `.\gradlew.bat :feature_task:assembleDebug` |
| 路由模块单元测试 | `./gradlew :component_nav:testDebugUnitTest` | `.\gradlew.bat :component_nav:testDebugUnitTest` |
| 停止 Gradle 守护进程 | `./gradlew --stop` | `.\gradlew.bat --stop` |

**PowerShell 提示**

- 当前目录应在仓库根（含 `gradlew.bat`）；路径使用 `.\gradlew.bat`，不要用 `/d`（那是 CMD 的 `cd` 参数）。
- 若执行策略限制脚本，可直接调用 `gradlew.bat`，无需 `.\` 以外的 PowerShell 脚本。

**CMD 提示**

```bat
cd /d D:\path\to\ZhtTaskFlow
gradlew.bat :app:assembleDebug
```

---

## Windows：`clean` 与 lint-cache 文件锁

### 现象

在 Windows 上执行 **`app:clean`** 或根任务 **`clean`** 时，偶发失败，日志中出现无法删除目录/文件、**`lint-cache`** 或 **`intermediates\lint-cache`** 被占用（`Access is denied`、`The process cannot access the file` 等）。

常见原因：

- Android Studio 后台 Lint / 索引仍占用 `build` 下缓存；
- 上一次构建的 **Gradle Daemon** 未释放文件句柄；
- 杀毒/同步盘对 `build` 目录实时扫描导致短暂锁。

该问题**与业务代码无关**，在 Windows 上较常见；Linux/macOS 上较少见。

### CI 规避（推荐）

流水线以**可重复编译**为目标时，**不必每次 `clean`**：

- 日常校验：`.\gradlew.bat :app:compileDebugKotlin`、`:app:assembleDebug`、`:app:lintVitalRelease`（按流水线需要选取）；
- 仅在怀疑缓存污染、切换分支或大版本升级后再执行 `clean`。

这样可减少 Windows 代理上对 `build` 目录的并发删除，降低 lint-cache 锁概率。

### 本地遇到锁时的处理步骤

按顺序尝试，通常前两步即可恢复。

**1. 停止 Gradle 守护进程**

```bat
.\gradlew.bat --stop
```

**2. 关闭 Android Studio 中对本工程的占用**（或 File → Invalidate Caches 前可先关工程），确认无残留 `java.exe` 构建进程。

**3. 手动删除 lint-cache（仅删缓存，不删源码）**

PowerShell（仓库根目录）：

```powershell
# 集成壳 app 模块（最常见）
Remove-Item -Recurse -Force ".\app\build\intermediates\lint-cache" -ErrorAction SilentlyContinue

# 若其他模块 clean 也报 lint-cache，可批量删除（仍在仓库根）
Get-ChildItem -Path . -Directory -Recurse -Filter "lint-cache" -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -match "\\build\\intermediates\\lint-cache$" } |
    Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
```

CMD（仓库根目录）：

```bat
rmdir /s /q app\build\intermediates\lint-cache 2>nul
```

**4. 再执行构建（可先不 clean）**

```bat
.\gradlew.bat :app:compileDebugKotlin
```

若仍失败，可再删除整个模块 `build` 目录后重试（仍勿删 `.gradle` 全局缓存除非明确需要）：

```powershell
Remove-Item -Recurse -Force ".\app\build" -ErrorAction SilentlyContinue
```

### 仍无法删除时

- 在任务管理器中结束仍占用工程的 **Gradle Daemon** / **Kotlin Compile Daemon**；
- 将工程目录加入杀毒/OneDrive **排除项**（`build`、`.gradle`）；
- 最后手段：重启系统后仅执行 **不带 `clean`** 的 `assemble` / `compile`。

---

## 常用本地构建命令（Windows 复制即用）

```bat
:: 日常开发：编译集成壳 Debug
.\gradlew.bat :app:compileDebugKotlin

:: 打可安装 Debug APK
.\gradlew.bat :app:assembleDebug

:: 发布前 Lint（Release 关键路径）
.\gradlew.bat :app:lintVitalRelease

:: 单模块独立调试（以 feature_task 为例）
.\gradlew.bat :feature_task:assembleDebug

:: 需要全量清缓存且未遇到文件锁时
.\gradlew.bat clean :app:compileDebugKotlin
```

更多架构级校验命令见 [ARCHITECTURE.md § 校验](ARCHITECTURE.md) 与 [TASKFLOW_CUSTOM_LINT.md](TASKFLOW_CUSTOM_LINT.md)。

---

## 相关文档

| 文档 | 内容 |
|------|------|
| [ARCHITECTURE.md](ARCHITECTURE.md) | 模块边界、MVI、壳层注入、构建校验清单 |
| [TASKFLOW_ROUTE_GATES.md](TASKFLOW_ROUTE_GATES.md) | 路由门禁与深链手动验证 |
| [TASKFLOW_CUSTOM_LINT.md](TASKFLOW_CUSTOM_LINT.md) | 自定义 Lint 与模块 lint 任务 |
