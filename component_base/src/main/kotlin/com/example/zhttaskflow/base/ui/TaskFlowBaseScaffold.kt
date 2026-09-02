package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.zhttaskflow.base.analytics.TaskFlowAnalytics
import com.example.zhttaskflow.base.analytics.TaskFlowAnalyticsCompositionRoot
import com.example.zhttaskflow.base.analytics.rememberTaskFlowDebugAnalytics
import com.example.zhttaskflow.base.exception.TaskFlowCrashReporter
import com.example.zhttaskflow.base.exception.TaskFlowExceptionMonitoringRoot
import com.example.zhttaskflow.base.performance.TaskFlowDebugPerformanceReporter
import com.example.zhttaskflow.base.performance.TaskFlowPerformanceCompositionRoot
import com.example.zhttaskflow.base.performance.TaskFlowPerformanceReporter
import com.example.zhttaskflow.base.performance.TaskFlowPerformanceScaffoldBindings
import com.example.zhttaskflow.base.performance.rememberTaskFlowDebugPerformance
import com.example.zhttaskflow.base.performance.rememberTaskFlowPerformance
import com.example.zhttaskflow.base.ext.LocalTaskFlowDialogController
import com.example.zhttaskflow.base.ext.LocalTaskFlowLoadingController
import com.example.zhttaskflow.base.ext.LocalTaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ext.LocalTaskFlowSnackbarHostState
import com.example.zhttaskflow.base.ext.TaskFlowDialogController
import com.example.zhttaskflow.base.ext.TaskFlowLoadingController
import com.example.zhttaskflow.base.ext.TaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ui.dialog.TaskFlowDialogHost

/**
 * 壳层/页面层共用的全局交互宿主（Snackbar / Loading / Dialog）。
 */
internal data class TaskFlowScaffoldGlobalHosts(
    val snackbarHostState: SnackbarHostState,
    val snackbarDispatcher: TaskFlowSnackbarDispatcher,
    val loadingController: TaskFlowLoadingController,
    val dialogController: TaskFlowDialogController,
)

/**
 * 若组合树上游已由 [TaskFlowBaseScaffold] 注入全局宿主，则返回该宿主以供内层脚手架复用。
 */
@Composable
internal fun taskFlowParentGlobalHostsOrNull(): TaskFlowScaffoldGlobalHosts? {
    val snackbarDispatcher = runCatching { LocalTaskFlowSnackbarDispatcher.current }.getOrNull()
        ?: return null
    val snackbarHostState = runCatching { LocalTaskFlowSnackbarHostState.current }.getOrNull()
        ?: return null
    val loadingController = runCatching { LocalTaskFlowLoadingController.current }.getOrNull()
        ?: return null
    val dialogController = runCatching { LocalTaskFlowDialogController.current }.getOrNull()
        ?: return null
    return TaskFlowScaffoldGlobalHosts(
        snackbarHostState = snackbarHostState,
        snackbarDispatcher = snackbarDispatcher,
        loadingController = loadingController,
        dialogController = dialogController,
    )
}

/**
 * 核心页面脚手架：统一 [TaskFlowInsetsPolicy]、系统栏与内容区边距，不含顶栏/导航等业务层级 UI。
 *
 * 全局 Snackbar / Loading / Dialog 采用**单宿主**策略：外层（通常为 App 壳）创建并展示宿主 UI；
 * 内层再次调用本组件时自动检测并**继承**父级 CompositionLocal，不再重复创建宿主或 SnackbarHost，
 * 保证路由拦截与页面 MVI 提示共用同一队列与层级。
 *
 * 独立调试等无外层宿主场景下，本组件自动降级为本地宿主创建模式。
 *
 * @param analytics 壳层注入的埋点实现；为 `null` 时使用 [com.example.zhttaskflow.base.analytics.rememberTaskFlowDebugAnalytics]。
 * @param performanceImpl 壳层注入的 APM 实现；为 `null` 时使用 [TaskFlowDebugPerformanceReporter] 经 [com.example.zhttaskflow.base.performance.rememberTaskFlowDebugPerformance] 装配。
 * @param crashReporter 壳层注入的崩溃上报；为 `null` 时使用 [com.example.zhttaskflow.base.exception.TaskFlowDebugCrashReporter]。
 * 页面性能（首帧 / 滚动 FPS / 停留）由 [com.example.zhttaskflow.base.performance.TaskFlowPerformanceCompositionRoot] 注入，
 * 并与 [com.example.zhttaskflow.base.ui.extension.PageLifecycleLog] 的 `pageName` 关联。
 * @see com.example.zhttaskflow.base.doc.TaskFlowBaseArchitecture
 */
@Composable
fun TaskFlowBaseScaffold(
    modifier: Modifier = Modifier,
    consumeStatusBarsInContent: Boolean,
    analytics: TaskFlowAnalytics? = null,
    performanceImpl: TaskFlowPerformanceReporter? = null,
    crashReporter: TaskFlowCrashReporter? = null,
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    header: @Composable () -> Unit = {},
    contentModifier: Modifier = Modifier,
    content: @Composable (scaffoldContentPadding: PaddingValues) -> Unit,
) {
    val parentHosts = taskFlowParentGlobalHostsOrNull()
    if (parentHosts != null) {
        TaskFlowBaseScaffoldContent(
            modifier = modifier,
            consumeStatusBarsInContent = consumeStatusBarsInContent,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            header = header,
            contentModifier = contentModifier,
            hosts = parentHosts,
            ownsGlobalHosts = false,
            content = content,
        )
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val snackbarDispatcher = remember(snackbarHostState, snackbarScope) {
        TaskFlowSnackbarDispatcher(
            hostState = snackbarHostState,
            scope = snackbarScope,
        )
    }
    val loadingController = remember { TaskFlowLoadingController() }
    val dialogController = remember { TaskFlowDialogController() }
    val localHosts = remember(
        snackbarHostState,
        snackbarDispatcher,
        loadingController,
        dialogController,
    ) {
        TaskFlowScaffoldGlobalHosts(
            snackbarHostState = snackbarHostState,
            snackbarDispatcher = snackbarDispatcher,
            loadingController = loadingController,
            dialogController = dialogController,
        )
    }

    DisposableEffect(loadingController) {
        onDispose { loadingController.hideLoading() }
    }
    DisposableEffect(dialogController) {
        onDispose { dialogController.dismissAll() }
    }

    val resolvedAnalytics = analytics ?: rememberTaskFlowDebugAnalytics()
    val resolvedPerformanceReporter = performanceImpl ?: TaskFlowDebugPerformanceReporter
    val performance = rememberTaskFlowDebugPerformance(reporter = resolvedPerformanceReporter)

    CompositionLocalProvider(
        LocalTaskFlowSnackbarHostState provides snackbarHostState,
        LocalTaskFlowSnackbarDispatcher provides snackbarDispatcher,
        LocalTaskFlowLoadingController provides loadingController,
        LocalTaskFlowDialogController provides dialogController,
    ) {
        TaskFlowPerformanceCompositionRoot(performance = performance) {
            TaskFlowAnalyticsCompositionRoot(analytics = resolvedAnalytics) {
                TaskFlowExceptionMonitoringRoot(
                    snackbarDispatcher = snackbarDispatcher,
                    crashReporter = crashReporter,
                ) {
                    TaskFlowBaseScaffoldContent(
                        modifier = modifier,
                        consumeStatusBarsInContent = consumeStatusBarsInContent,
                        bottomBar = bottomBar,
                        floatingActionButton = floatingActionButton,
                        header = header,
                        contentModifier = contentModifier,
                        hosts = localHosts,
                        ownsGlobalHosts = true,
                        content = content,
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskFlowBaseScaffoldContent(
    modifier: Modifier,
    consumeStatusBarsInContent: Boolean,
    bottomBar: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    header: @Composable () -> Unit,
    contentModifier: Modifier,
    hosts: TaskFlowScaffoldGlobalHosts,
    ownsGlobalHosts: Boolean,
    content: @Composable (PaddingValues) -> Unit,
) {
    val performance = rememberTaskFlowPerformance()
    val performanceContentModifier = TaskFlowPerformanceScaffoldBindings(
        performance = performance,
        contentModifier = contentModifier,
    )
    val pageBackground = TaskFlowPageBackground.color()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBackground),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = pageBackground,
            contentWindowInsets = TaskFlowInsetsPolicy.scaffoldContentWindowInsets,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            snackbarHost = {
                if (ownsGlobalHosts) {
                    TaskFlowSnackbarHost(hostState = hosts.snackbarHostState)
                }
            },
        ) { innerPadding ->
            val contentInsets = rememberTaskFlowScaffoldContentPadding(scaffoldPadding = innerPadding)
            Column(modifier = Modifier.fillMaxSize()) {
                header()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(pageBackground)
                        .then(
                            if (consumeStatusBarsInContent) {
                                Modifier.statusBarsPadding()
                            } else {
                                Modifier
                            },
                        )
                        .then(performanceContentModifier)
                        .padding(contentInsets),
                ) {
                    content(contentInsets)
                }
            }
        }
        if (ownsGlobalHosts) {
            val loadingState = hosts.loadingController.uiState
            TaskFlowBlockingLoadingOverlay(
                visible = loadingState.visible,
                message = loadingState.message,
            )
            TaskFlowDialogHost(controller = hosts.dialogController)
        }
    }
}
