package com.example.zhttaskflow.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** 接入 [component_lint] 自定义规则：App / Feature 模块执行 Lint 时加载。 */
internal fun Project.configureTaskFlowCustomLint() {
    val attachLintChecks = path == ":app" || path.startsWith(":feature_")
    if (!attachLintChecks) {
        return
    }
    dependencies {
        add("lintChecks", project(":component_lint"))
    }
}

/** Release（lintVital）阻断项；规则默认 Severity.WARNING，Debug 仅警告。 */
internal val TaskFlowCustomLintFatalIssueIds: List<String> = listOf(
    "TaskFlowNoToastInFeature",
    "TaskFlowNoBareScaffoldInFeature",
    "TaskFlowLogUiInteractionMissingPageId",
)
