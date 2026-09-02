package com.example.zhttaskflow.lint

import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestMode
import org.junit.Test

class TaskFlowNoToastInFeatureDetectorTest {

    @Test
    fun toastImportInFeature_reportsIssue() {
        lintTask()
            .issues(TaskFlowNoToastInFeatureDetector.ISSUE)
            .files(
                kotlinSource(
                    """
                    package com.example.zhttaskflow.feature.home.presentation
                    import android.widget.Toast
                    fun demo() {
                        Toast.makeText(null, "x", Toast.LENGTH_SHORT).show()
                    }
                    """.trimIndent(),
                    "src/com/example/zhttaskflow/feature/home/presentation/ToastDemo.kt",
                ),
                androidToastStub(),
            )
            .run()
            .expectContains("TaskFlowNoToastInFeature")
    }
}

class TaskFlowNoBareScaffoldInFeatureDetectorTest {

    @Test
    fun bareScaffoldInFeature_reportsIssue() {
        lintTask()
            .issues(TaskFlowNoBareScaffoldInFeatureDetector.ISSUE)
            .files(
                kotlinSource(
                    """
                    package com.example.zhttaskflow.feature.task.presentation
                    import androidx.compose.material3.Scaffold
                    fun badScreen() {
                        Scaffold { }
                    }
                    """.trimIndent(),
                    "src/com/example/zhttaskflow/feature/task/presentation/BadScreen.kt",
                ),
                composeScaffoldStub(),
            )
            .allowDuplicates()
            .run()
            .expectContains("TaskFlowNoBareScaffoldInFeature")
    }
}

class TaskFlowLogUiInteractionMissingPageIdDetectorTest {

    @Test
    fun logUiInteractionWithoutPageId_reportsIssue() {
        lintTask()
            .issues(TaskFlowLogUiInteractionMissingPageIdDetector.ISSUE)
            .files(
                kotlinSource(
                    """
                    package com.example.zhttaskflow.feature.home.presentation
                    import com.example.zhttaskflow.base.ui.extension.logUiInteraction
                    fun demo() {
                        logUiInteraction(action = "click", identifier = "home_btn")
                    }
                    """.trimIndent(),
                    "src/com/example/zhttaskflow/feature/home/presentation/LogDemo.kt",
                ),
                logUiInteractionStub(),
            )
            .run()
            .expectContains("TaskFlowLogUiInteractionMissingPageId")
    }

    @Test
    fun logUiInteractionWithPageId_noIssue() {
        lintTask()
            .issues(TaskFlowLogUiInteractionMissingPageIdDetector.ISSUE)
            .files(
                kotlinSource(
                    """
                    package com.example.zhttaskflow.feature.home.presentation
                    import com.example.zhttaskflow.base.ui.extension.logUiInteraction
                    private const val HOME_PAGE_ID = "Home"
                    fun demo() {
                        logUiInteraction(
                            action = "click",
                            identifier = "home_btn",
                            pageId = HOME_PAGE_ID,
                        )
                    }
                    """.trimIndent(),
                    "src/com/example/zhttaskflow/feature/home/presentation/LogDemoOk.kt",
                ),
                logUiInteractionStub(),
            )
            .run()
            .expectClean()
    }
}

private fun lintTask(): TestLintTask {
    return TestLintTask()
        .testModes(TestMode.DEFAULT)
        .allowCompilationErrors()
        .allowMissingSdk()
}

private fun kotlinSource(source: String, relativePath: String): TestFile {
    return TestFiles.kotlin(relativePath, source)
}

private fun composeScaffoldStub(): TestFile {
    return TestFiles.kotlin(
        "stubs/androidx/compose/material3/Scaffold.kt",
        """
        package androidx.compose.material3
        import androidx.compose.runtime.Composable
        @Composable
        fun Scaffold(content: @Composable () -> Unit) {}
        """.trimIndent(),
    )
}

private fun composableAnnotationStub(): TestFile {
    return TestFiles.kotlin(
        "stubs/androidx/compose/runtime/Composable.kt",
        """
        package androidx.compose.runtime
        annotation class Composable
        """.trimIndent(),
    )
}

private fun androidToastStub(): TestFile {
    return TestFiles.java(
        "src/android/widget/Toast.java",
        """
        package android.widget;
        public class Toast {
            public static final int LENGTH_SHORT = 0;
            public static Toast makeText(Object context, CharSequence text, int duration) {
                return new Toast();
            }
            public void show() {}
        }
        """.trimIndent(),
    )
}

private fun logUiInteractionStub(): TestFile {
    return TestFiles.kotlin(
        "src/com/example/zhttaskflow/base/ui/extension/ComposeInteractionLogging.kt",
        """
        package com.example.zhttaskflow.base.ui.extension
        fun logUiInteraction(
            action: String,
            identifier: String,
            pageId: String? = null,
            params: Map<String, String?>? = null,
            detail: String? = null,
            tag: String = "UiClick",
        ) {}
        """.trimIndent(),
    )
}
