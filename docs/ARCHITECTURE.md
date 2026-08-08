# ZhtTaskFlow 架构说明

## 1. 目标

多模块工程骨架：Feature 垂直 DDD + Clean Architecture（`domain` / `data` / `presentation` 内嵌于各 Feature）+ MVI 约定（仅文档，本仓库无 UiState/UiEvent 实现）。本阶段无 DI（无 Hilt/KSP）。

## 2. 模块职责

| 模块 | namespace（自动） | 职责 |
|------|-------------------|------|
| app | com.example.zhttaskflow | 壳工程、条件集成 Feature |
| component_base | com.example.zhttaskflow.base | 资源前缀与 Library 基座（通用 api 依赖由 library 插件注入） |
| component_core | com.example.zhttaskflow.core | Retrofit/Room api，OkHttp 隐藏 |
| component_nav | com.example.zhttaskflow.nav | Compose/Navigation/lifecycle api 基座 |
| feature_task | com.example.zhttaskflow.feature.task | 业务自治单元（当前仅空目录） |
| feature_article | com.example.zhttaskflow.feature.article | 同上 |

所有 Android Library 模块（含 `component_base` / `component_core` / `component_nav` 及 Feature 的 library 模式）均在模块根目录预留 `consumer-rules.pro` 占位；由 `ConfigureAndroidCommon` 在 `defaultConfig` 中统一配置 `consumerProguardFiles("consumer-rules.pro")`，library 构建时自动生效，避免业务阶段编写规则却未接入 AAR 的隐患。

禁止顶层 `lib-domain`、`lib-data`。

## 3. SDK 与工具链

- AGP 8.7.3，Gradle Wrapper 8.9
- compileSdk 35，targetSdk 35，minSdk 24（全模块由 ConfigureAndroidCommon 统一）
- Kotlin 2.0 + K2（`kotlin.experimental.tryK2=true`），JDK 17

## 4. namespace 自动生成

- 根包固定 `com.example.zhttaskflow`
- 去掉模块 path 前导 `:`，剔除 `component_` 前缀，`_` 替换为 `.`，拼接到根包后
- `:app` → `com.example.zhttaskflow`
- 由 `taskFlow.android.common` 在配置阶段写入 `android.namespace`

## 5. 依赖流向（允许）

- app → component_nav；条件 → feature_*（**不直接**依赖 component_base，经 nav 的 api 传递获得 base）
- component_nav **api** → component_base（base 能力向上传递）
- component_core **api** → component_base
- feature_* → component_core、component_nav（**不直接**依赖 component_base，经 api 传递获得 base）

## 6. 禁止依赖

- feature ↔ feature
- base / core / nav → feature 或 app
- core → nav
- core → 任意 Compose 坐标

## 7. api / implementation

- **TaskFlowAndroidLibraryPlugin**（所有 Library）：api → kotlinx-coroutines-core/android、androidx-core-ktx
- **component_core**（约定插件）：**api** → component_base、retrofit、room-runtime、room-ktx；implementation → okhttp、okhttp-logging
- **component_nav**（约定插件）：**api** → component_base、lifecycle-runtime-ktx、Compose BOM 及 UI 栈（含 ui-tooling-preview）
- **Feature 插件**：implementation → component_core、component_nav（base 经 core/nav 的 api 传递）

Kotlin 标准库不由 Version Catalog 声明，由 `org.jetbrains.kotlin.android` 插件对齐 Kotlin 版本。

测试依赖（junit、coroutines-test、compose-ui-test-junit4）版本已在 Catalog 预留；`ConfigureTestDependencies` 注释块待业务阶段启用。

## 8. Feature 双模式

| 模式 | gradle.properties | Android 插件 | 源码 / Manifest |
|------|-------------------|--------------|-----------------|
| library | `feature.*.standalone=false` | library + Compose（内含 base） | 仅 `src/main`；manifest 为空根；**不编译** standalone |
| application | `true` | application + kotlin + compose + base | `src/main` 保留默认目录；**追加** standalone kotlin/res；manifest **仅** standalone 文件 |

开关键名固定：`:feature_task` → `feature.task.standalone`，不可在 `taskFlowFeature` DSL 配置。

`applicationId` 可在模块 `taskFlowFeature { applicationId.set(...) }` 覆盖；standalone 模式下插件在 `configure<ApplicationExtension>` 内直接写入 `defaultConfig.applicationId`（避免 AGP「已读取 applicationId」报错）。

app 在 `app/build.gradle.kts` 中当 standalone=true 时不 `implementation` 对应 feature。

## 9. component_base 为 Android 模块的取舍

需要 `androidx.core` 与资源前缀约束时保留 Android Library。纯 JVM 领域逻辑应放在 feature 的 `domain` 包，且禁止 Android/Compose import。

**domain 依赖约束（业务阶段）**：domain 仅依赖 Kotlin + 协程；禁止 `android.*`、`androidx.compose.*` import；不要从 domain 引用 `component_nav` 的 Compose。

**纯 JVM 拆分思路**：后续可拆 `component_common`（pure Kotlin）+ `component_android_base`，feature domain 仅依赖 common。

## 10. MVI（业务阶段）

- UiState：不可变
- UiEvent：输入
- UiEffect：一次性副作用
- 本骨架不生成上述类型

## 11. 资源前缀

app_、base_、core_、nav_、task_、article_；`values` 中名称必须带前缀。Feature 模块通过 `taskFlow { resourcePrefix.set(...) }` 配置。

`ConfigureAndroidCommon` Lint 策略：

- `MissingPrefix` → **error**（资源前缀强制）
- 关闭骨架期噪声：`MissingTranslation`、`IconMissingDensityFolder`、`HardcodedText`
- `abortOnError = true`；各模块根目录 `lint-baseline.xml` 占位，可 `updateLintBaseline` 生成基线

## 12. 约定插件应用顺序

- **Library 栈**：`com.android.library` → `org.jetbrains.kotlin.android` → `taskFlow.android.common`（`TaskFlowAndroidLibraryPlugin` 一次完成）
- **App 栈**：`com.android.application` → `kotlin.android` → `kotlin.compose` → `taskFlow.android.common`
- **Feature library**：`taskFlow.android.library` → `kotlin.compose`（**禁止**再 apply base）
- **Feature application**：与 App 栈相同，不经过 `taskFlow.android.library`

## 13. Room schemas 目录

`component_core/schemas/` 当前为 `.gitkeep` 占位。接入 KSP 与 `room-compiler` 后，在 `TaskFlowAndroidCorePlugin` 中配置 `room.schemaLocation` 指向该目录；编译期按 Entity 变更自动生成 JSON，**须纳入版本管理**并与 `room` 版本（仅 toml）保持一致。

## 14. 后续接入 Hilt

1. 在 `libs.versions.toml` 增加 hilt、ksp、room-compiler
2. 新增或叠加 `taskFlow.android.hilt` 约定插件
3. Application 使用 `@HiltAndroidApp`；Module 放在 `component_core/di/`
4. 启用 Core 插件中 Room schema 注释块，`schemas/` 目录已预留

## 15. 配置缓存与 Provider.get()

- `gradle.properties` 预留 `org.gradle.configuration-cache=true`（默认注释）。全模块 `assembleDebug` 通过后再开启，可缩短增量配置时间。
- **权衡**：`TaskFlowAndroidFeaturePlugin` 在 `apply` 阶段对 standalone 开关使用 `Provider.get()` 同步求值，以便立即分支选择 Android 插件类型；`applicationId` 在 `configure<ApplicationExtension>` 内从 `taskFlowFeature` 扩展读取并写入 `defaultConfig`（与模块 DSL 约定一致，且早于 AGP 读取 applicationId）。
- 建议流程：先关闭配置缓存完成首次全量构建 → 开启配置缓存 → 重复 assemble 与切换 `feature.*.standalone` 回归。

## 16. 构建验证

./gradlew :app:assembleDebug
./gradlew :feature_task:assembleDebug :feature_article:assembleDebug
./gradlew checkDependencyRules
./gradlew lintDebug
