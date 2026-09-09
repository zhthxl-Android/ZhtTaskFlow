package com.example.zhttaskflow.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.client.api.UElementHandler
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.getContainingUFile

/**
 * Feature 业务层禁止直接使用 Material3 裸组件，须使用 `component_base` 封装（[ListScaffold]、[PageScaffold]、[Divider]、[SnackbarHost]、[PullToRefreshBox] 等）。
 */
class NoBareScaffoldInFeatureDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UImportStatement::class.java, UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler = object : UElementHandler() {
        override fun visitImportStatement(node: UImportStatement) {
            if (!isFeatureLintScope(context.project.name, node.getContainingUFile()?.packageName)) {
                return
            }
            val importReference = node.importReference?.asSourceString().orEmpty()
            val violation = forbiddenImportMessage(importReference)
            if (violation != null) {
                reportBareMaterialComponent(context, node, violation)
            }
        }

        override fun visitCallExpression(node: UCallExpression) {
            val violation = forbiddenCallMessage(node.methodName.orEmpty())
            if (violation == null) {
                return
            }
            reportBareMaterialCallIfNeeded(context, node, violation)
        }
    }

    private fun forbiddenImportMessage(importReference: String): String? {
        if (matchesImport(importReference, "Scaffold")) {
            return "Scaffold"
        }
        if (
            matchesImport(importReference, "Divider") ||
            matchesImport(importReference, "HorizontalDivider") ||
            matchesImport(importReference, "VerticalDivider")
        ) {
            return "Divider"
        }
        if (matchesImport(importReference, "SnackbarHost")) {
            return "SnackbarHost"
        }
        if (importReference.contains("androidx.compose.material3.pulltorefresh.PullToRefreshBox")) {
            return "PullToRefreshBox"
        }
        return null
    }

    private fun matchesImport(importReference: String, simpleName: String): Boolean {
        return importReference == simpleName ||
            importReference.endsWith(".$simpleName") ||
            importReference.contains("androidx.compose.material3.$simpleName")
    }

    private fun forbiddenCallMessage(methodName: String): String? {
        return when (methodName) {
            "Scaffold" -> "Scaffold"
            "Divider", "HorizontalDivider", "VerticalDivider" -> "Divider"
            "SnackbarHost" -> "SnackbarHost"
            "PullToRefreshBox" -> "PullToRefreshBox"
            else -> null
        }
    }

    private fun reportBareMaterialCallIfNeeded(
        context: JavaContext,
        node: UCallExpression,
        componentLabel: String,
    ) {
        if (!isFeatureLintScope(context.project.name, node.getContainingUFile()?.packageName)) {
            return
        }
        val resolvedOwner = node.resolve()?.containingClass?.qualifiedName.orEmpty()
        if (resolvedOwner.isNotEmpty() && !isMaterial3Owner(resolvedOwner)) {
            return
        }
        reportBareMaterialComponent(context, node, componentLabel)
    }

    private fun isMaterial3Owner(qualifiedName: String): Boolean {
        return qualifiedName.contains("androidx.compose.material3") ||
            qualifiedName.contains("androidx.compose.material")
    }

    private fun reportBareMaterialComponent(
        context: JavaContext,
        node: UElement,
        componentLabel: String,
    ) {
        context.report(
            ISSUE,
            node,
            context.getLocation(node),
            "业务层禁止直接使用 Material3 $componentLabel，请使用 component_base 封装组件（如 ListScaffold、PageScaffold、Divider、SnackbarHost、PullToRefreshBox）。",
        )
    }

    companion object {
        private const val EXPLANATION = """
业务 Feature 的 Screen 须挂在统一壳层之上，并复用基础 UI 封装，以继承全局 Snackbar、Loading、Dialog、埋点与 inset 策略。

**修复建议**
- 一级 Tab 根列表页：使用 `ListScaffold` + `StateBox` / 列表封装；
- 二级详情或非 Tab 页：使用 `PageScaffold`；
- 分隔线：使用 `com.example.zhttaskflow.base.ui.Divider`；
- Snackbar 宿主：由 `BaseScaffold` 注入，业务使用 `LocalSnackbarDispatcher`；
- 下拉刷新：使用 `PullToRefreshBox`（`component_base`）；
- 仅 `component_base` 内 `BaseScaffold` 允许直接组合 Material `Scaffold` / `SnackbarHost`。
"""

        val ISSUE: Issue = Issue.create(
            id = LintIds.NO_BARE_SCAFFOLD_IN_FEATURE,
            briefDescription = "业务层禁止直接使用 Material3 裸 UI 组件",
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 8,
            severity = Severity.ERROR,
            implementation = Implementation(
                NoBareScaffoldInFeatureDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}
