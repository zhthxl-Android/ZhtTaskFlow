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
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UNamedExpression
import org.jetbrains.uast.getContainingUFile

/**
 * 强制 [logUiInteraction] 显式传入非空 [pageId]，与 [PageLifecycleLog] 的 pageName 对齐。
 */
class TaskFlowLogUiInteractionMissingPageIdDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler = object : UElementHandler() {
        override fun visitCallExpression(node: UCallExpression) {
            if (node.methodName != LOG_UI_INTERACTION) {
                return
            }
            reportMissingPageIdIfNeeded(context, node)
        }
    }

    private fun reportMissingPageIdIfNeeded(context: JavaContext, node: UCallExpression) {
        if (!isFeatureLintScope(context.project.name, node.getContainingUFile()?.packageName)) {
            return
        }
        val pageIdExpression = resolvePageIdArgument(node)
        if (pageIdExpression == null) {
            context.report(
                ISSUE,
                node,
                context.getLocation(node),
                "logUiInteraction 必须传入 pageId，且与 PageLifecycleLog 的 pageName 保持一致。",
            )
            return
        }
        if (pageIdExpression is ULiteralExpression && pageIdExpression.value == null) {
            context.report(
                ISSUE,
                pageIdExpression,
                context.getLocation(pageIdExpression),
                "logUiInteraction 的 pageId 不能为 null，请传入当前页面常量（如 HOME_PAGE_ID）。",
            )
        }
    }

    private fun resolvePageIdArgument(node: UCallExpression): org.jetbrains.uast.UExpression? {
        node.valueArguments.forEach { argument ->
            if (argument is UNamedExpression && argument.name == PAGE_ID_PARAM) {
                return argument.expression
            }
        }
        val positional = node.valueArguments.map { expr ->
            if (expr is UNamedExpression) expr.expression else expr
        }
        return if (positional.size >= 3) positional[2] else null
    }

    companion object {
        private const val LOG_UI_INTERACTION = "logUiInteraction"
        private const val PAGE_ID_PARAM = "pageId"

        private const val EXPLANATION = """
交互埋点须携带 pageId，保证与页面生命周期日志、产品 Analytics 字段一致。

**修复建议**
- 为 Screen 定义 `private const val XXX_PAGE_ID = "..."`；
- 调用 `logUiInteraction(..., pageId = XXX_PAGE_ID, ...)`；
- 列表项点击优先使用 `listItemClickWithLog`（pageId 为必填参数）。
"""

        val ISSUE: Issue = Issue.create(
            id = TaskFlowLintIds.LOG_UI_INTERACTION_MISSING_PAGE_ID,
            briefDescription = "logUiInteraction 缺少 pageId",
            explanation = EXPLANATION,
            category = Category.CORRECTNESS,
            priority = 9,
            severity = Severity.ERROR,
            implementation = Implementation(
                TaskFlowLogUiInteractionMissingPageIdDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )
    }
}
