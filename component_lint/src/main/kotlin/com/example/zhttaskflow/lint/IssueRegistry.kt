package com.example.zhttaskflow.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API

/**
 * ZhtTaskFlow 工程自定义 Lint 规则注册表。
 *
 * 三条规则均为 [com.android.tools.lint.detector.api.Severity.ERROR]；
 * 根工程 [com.example.zhttaskflow.buildlogic.ConfigureAndroidCommon] 同步 `fatal` / `error`，Debug 与 Release Lint 均阻断。
 */
class IssueRegistry : IssueRegistry() {

    override val issues = listOf(
        NoToastInFeatureDetector.ISSUE,
        NoBareScaffoldInFeatureDetector.ISSUE,
        LogUiInteractionMissingPageIdDetector.ISSUE,
    )

    override val api: Int = CURRENT_API

    override val minApi: Int = CURRENT_API

    override val vendor: Vendor = Vendor(
        vendorName = "ZhtTaskFlow",
        feedbackUrl = "https://github.com/example/zhttaskflow/issues",
        contact = "taskflow-arch",
    )
}
