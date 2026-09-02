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
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.getContainingUFile

/**
 * Feature 模块禁止直接调用 [android.widget.Toast]，统一走壳层 [com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold] Snackbar。
 */
class TaskFlowNoToastInFeatureDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UImportStatement::class.java, UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler = object : UElementHandler() {
        override fun visitImportStatement(node: UImportStatement) {
            if (!isFeatureLintScope(context.project.name, node.getContainingUFile()?.packageName)) {
                return
            }
            val importReference = node.importReference?.asSourceString().orEmpty()
            if (
                importReference == "Toast" ||
                importReference == "android.widget.Toast" ||
                importReference.endsWith(".Toast")
            ) {
                context.report(
                    ISSUE,
                    node,
                    context.getLocation(node),
                    "Feature 层禁止直接使用 Toast，请通过 ViewModel 派发 UiEffect 或壳层 Snackbar 展示提示。",
                )
            }
        }

        override fun visitCallExpression(node: UCallExpression) {
            if (!isFeatureLintScope(context.project.name, node.getContainingUFile()?.packageName)) {
                return
            }
            val method = node.resolve() as? PsiMethod ?: return
            val containingClass = method.containingClass?.qualifiedName.orEmpty()
            if (containingClass == "android.widget.Toast") {
                context.report(
                    ISSUE,
                    node,
                    context.getLocation(node),
                    "Feature 层禁止直接调用 Toast API（${method.name}），请使用全局 Snackbar（TaskFlowBaseScaffold / UiEffect）。",
                )
            }
        }
    }

    companion object {
        private const val EXPLANATION = """
Feature 业务模块不得直接依赖 Android Toast，以保证提示样式、时长与埋点与集成宿主一致。

**修复建议**
- 在 MVI 中通过 `UiEffect.ShowSnackbar`（或项目内等价 Effect）触发提示；
- 由 `TaskFlowBaseScaffold` 的 `TaskFlowSnackbarHost` 统一展示；
- 需要阻塞反馈时使用 `TaskFlowBlockingLoadingOverlay` 或 Dialog，而非 Toast。
"""

        val ISSUE: Issue = Issue.create(
            id = TaskFlowLintIds.NO_TOAST_IN_FEATURE,
            briefDescription = "Feature 层禁止直接使用 Toast",
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 8,
            severity = Severity.WARNING,
            implementation = Implementation(
                TaskFlowNoToastInFeatureDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}
