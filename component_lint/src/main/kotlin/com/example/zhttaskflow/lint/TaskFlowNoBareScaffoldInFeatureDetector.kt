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
 * Feature 业务层禁止直接使用 Material `Scaffold`，须使用 [TaskFlowListScaffold] / [TaskFlowScaffold] 等壳组件。
 */
class TaskFlowNoBareScaffoldInFeatureDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UImportStatement::class.java, UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler = object : UElementHandler() {
        override fun visitImportStatement(node: UImportStatement) {
            if (!isFeatureLintScope(context.project.name, node.getContainingUFile()?.packageName)) {
                return
            }
            val importReference = node.importReference?.asSourceString().orEmpty()
            if (isForbiddenScaffoldImport(importReference)) {
                reportBareScaffold(context, node)
            }
        }

        override fun visitCallExpression(node: UCallExpression) {
            if (node.methodName != "Scaffold") {
                return
            }
            reportBareScaffoldIfNeeded(context, node)
        }
    }

    private fun isForbiddenScaffoldImport(importReference: String): Boolean {
        return importReference == "Scaffold" ||
            importReference.endsWith(".Scaffold") ||
            importReference.contains("androidx.compose.material3.Scaffold") ||
            importReference.contains("androidx.compose.material.Scaffold")
    }

    private fun reportBareScaffoldIfNeeded(context: JavaContext, node: UCallExpression) {
        if (!isFeatureLintScope(context.project.name, node.getContainingUFile()?.packageName)) {
            return
        }
        val resolvedOwner = node.resolve()?.containingClass?.qualifiedName.orEmpty()
        if (
            resolvedOwner.isNotEmpty() &&
            !resolvedOwner.contains("androidx.compose.material3") &&
            !resolvedOwner.contains("androidx.compose.material")
        ) {
            return
        }
        reportBareScaffold(context, node)
    }

    private fun reportBareScaffold(context: JavaContext, node: UElement) {
        context.report(
            ISSUE,
            node,
            context.getLocation(node),
            "业务层禁止创建裸 Scaffold，请使用 TaskFlowListScaffold（一级 Tab 列表）或 TaskFlowScaffold（二级页）。",
        )
    }

    companion object {
        private const val EXPLANATION = """
业务 Feature 的 Screen 须挂在统一壳层之上，以继承全局 Snackbar、Loading、Dialog、埋点与 inset 策略。

**修复建议**
- 一级 Tab 根列表页：使用 `TaskFlowListScaffold` + `StateBox` / 列表封装；
- 二级详情或非 Tab 页：使用 `TaskFlowScaffold`；
- 仅 `component_base` 内 `TaskFlowBaseScaffold` 允许直接组合 Material `Scaffold`。
"""

        val ISSUE: Issue = Issue.create(
            id = TaskFlowLintIds.NO_BARE_SCAFFOLD_IN_FEATURE,
            briefDescription = "业务层禁止直接使用裸 Scaffold",
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 8,
            severity = Severity.ERROR,
            implementation = Implementation(
                TaskFlowNoBareScaffoldInFeatureDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}
