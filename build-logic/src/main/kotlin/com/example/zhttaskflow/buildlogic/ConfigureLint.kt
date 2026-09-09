package com.example.zhttaskflow.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** 接入 [component_lint] 自定义规则：App / Feature 模块执行 Lint 时加载。 */
internal fun Project.configureCustomLint() {
    val attachLintChecks = path == ":app" || path.startsWith(":feature_")
    if (!attachLintChecks) {
        return
    }
    dependencies {
        add("lintChecks", project(":component_lint"))
    }
}

/** 工程自定义 Lint（Severity.ERROR）；Debug / Release Lint 与 lintVital 均须零违规。Issue ID 为构建契约，勿改。 */
internal val CustomLintFatalIssueIds: List<String> = listOf(
    "TaskFlowNoToastInFeature",
    "TaskFlowNoBareScaffoldInFeature",
    "TaskFlowLogUiInteractionMissingPageId",
)
