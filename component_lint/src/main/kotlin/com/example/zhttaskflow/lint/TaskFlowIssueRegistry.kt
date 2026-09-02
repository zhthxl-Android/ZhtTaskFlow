package com.example.zhttaskflow.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

/**
 * TaskFlow 工程自定义 Lint 规则注册表。
 *
 * 默认严重级别为 [com.android.tools.lint.detector.api.Severity.WARNING]；
 * Release 阻断由根工程 `fatal` 配置 + `lintVitalRelease` 生效，Debug 构建仅提示警告。
 */
class TaskFlowIssueRegistry : IssueRegistry() {

    override val issues = listOf(
        TaskFlowNoToastInFeatureDetector.ISSUE,
        TaskFlowNoBareScaffoldInFeatureDetector.ISSUE,
        TaskFlowLogUiInteractionMissingPageIdDetector.ISSUE,
    )

    override val api: Int = CURRENT_API

    override val minApi: Int = CURRENT_API

    override val vendor: Vendor = Vendor(
        vendorName = "TaskFlow",
        feedbackUrl = "https://github.com/example/zhttaskflow/issues",
        contact = "taskflow-arch",
    )
}
