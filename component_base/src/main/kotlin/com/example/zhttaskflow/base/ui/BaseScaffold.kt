package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold as MaterialScaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.zhttaskflow.base.analytics.Analytics
import com.example.zhttaskflow.base.analytics.AnalyticsCompositionRoot
import com.example.zhttaskflow.base.analytics.rememberDebugAnalytics
import com.example.zhttaskflow.base.exception.CrashReporter
import com.example.zhttaskflow.base.exception.ExceptionMonitoringRoot
import com.example.zhttaskflow.base.exception.LocalCrashReporter
import com.example.zhttaskflow.base.exception.DebugCrashReporter
import com.example.zhttaskflow.base.exception.rememberCrashReporter
import com.example.zhttaskflow.base.observability.DeveloperObservability
import com.example.zhttaskflow.base.performance.DebugPerformanceReporter
import com.example.zhttaskflow.base.performance.PerformanceCompositionRoot
import com.example.zhttaskflow.base.performance.PerformanceReporter
import com.example.zhttaskflow.base.performance.PerformanceScaffoldBindings
import com.example.zhttaskflow.base.performance.rememberDebugPerformance
import com.example.zhttaskflow.base.performance.rememberPerformance
import com.example.zhttaskflow.base.ext.LocalDialogController
import com.example.zhttaskflow.base.ext.LocalLoadingController
import com.example.zhttaskflow.base.ext.LocalSnackbarDispatcher
import com.example.zhttaskflow.base.ext.LocalSnackbarHostState
import com.example.zhttaskflow.base.ext.DialogController
import com.example.zhttaskflow.base.ext.LoadingController
import com.example.zhttaskflow.base.ext.SnackbarDispatcher
import com.example.zhttaskflow.base.ui.dialog.DialogHost

/**
 * 壳层/页面层共用的全局交互宿主（Snackbar / Loading / Dialog）。
 */
internal data class ScaffoldGlobalHosts(
    val snackbarHostState: SnackbarHostState,
    val snackbarDispatcher: SnackbarDispatcher,
    val loadingController: LoadingController,
    val dialogController: DialogController,
)

/**
 * 若组合树上游已由 [BaseScaffold] 注入全局宿主，则返回该宿主以供内层脚手架复用。
 */
@Composable
internal fun parentGlobalHostsOrNull(): ScaffoldGlobalHosts? {
    val snackbarDispatcher = runCatching { LocalSnackbarDispatcher.current }.getOrNull()
        ?: return null
    val snackbarHostState = runCatching { LocalSnackbarHostState.current }.getOrNull()
        ?: return null
    val loadingController = runCatching { LocalLoadingController.current }.getOrNull()
        ?: return null
    val dialogController = runCatching { LocalDialogController.current }.getOrNull()
        ?: return null
    return ScaffoldGlobalHosts(
        snackbarHostState = snackbarHostState,
        snackbarDispatcher = snackbarDispatcher,
        loadingController = loadingController,
        dialogController = dialogController,
    )
}

/**
 * 核心页面脚手架：统一 [InsetsPolicy]、系统栏与内容区边距，不含顶栏/导航等业务层级 UI。
 *
 * 全局 Snackbar / Loading / Dialog 采用**单宿主**策略：外层（通常为 App 壳）创建并展示宿主 UI；
 * 内层再次调用本组件时自动检测并**继承**父级 CompositionLocal，不再重复创建宿主或 SnackbarHost，
 * 保证路由拦截与页面 MVI 提示共用同一队列与层级。
 *
 * 独立调试等无外层宿主场景下，本组件自动降级为本地宿主创建模式。
 *
 * @param analytics 壳层注入的埋点实现；为 `null` 时使用 [com.example.zhttaskflow.base.analytics.rememberDebugAnalytics]。
 * @param performanceImpl 壳层注入的 APM 实现；为 `null` 时使用 [DebugPerformanceReporter] 经 [com.example.zhttaskflow.base.performance.rememberDebugPerformance] 装配。
 * @param crashReporter 壳层注入的崩溃上报；为 `null` 时使用 [LocalCrashReporter] 默认（[DebugCrashReporter]）。
 * 页面性能（首帧 / 滚动 FPS / 停留）由 [com.example.zhttaskflow.base.performance.PerformanceCompositionRoot] 注入，
 * 并与 [com.example.zhttaskflow.base.ui.extension.PageLifecycleLog] 的 `pageName` 关联。
 * @see com.example.zhttaskflow.base.doc.BaseArchitecture
 */
@Composable
fun BaseScaffold(
    modifier: Modifier = Modifier,
    consumeStatusBarsInContent: Boolean,
    analytics: Analytics? = null,
    performanceImpl: PerformanceReporter? = null,
    crashReporter: CrashReporter? = null,
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    header: @Composable () -> Unit = {},
    contentModifier: Modifier = Modifier,
    content: @Composable (scaffoldContentPadding: PaddingValues) -> Unit,
) {
    val parentHosts = parentGlobalHostsOrNull()
    if (parentHosts != null) {
        BaseScaffoldContent(
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
        SnackbarDispatcher(
            hostState = snackbarHostState,
            scope = snackbarScope,
        )
    }
    val loadingController = remember { LoadingController() }
    val dialogController = remember { DialogController() }
    val localHosts = remember(
        snackbarHostState,
        snackbarDispatcher,
        loadingController,
        dialogController,
    ) {
        ScaffoldGlobalHosts(
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

    val shellAnalytics = analytics ?: rememberDebugAnalytics()
    val resolvedAnalytics = remember(shellAnalytics) {
        DeveloperObservability.wrapAnalytics(shellAnalytics)
    }
    val shellPerformanceReporter = performanceImpl ?: DebugPerformanceReporter
    val resolvedPerformanceReporter = remember(shellPerformanceReporter) {
        DeveloperObservability.wrapPerformanceReporter(shellPerformanceReporter)
    }
    val performance = rememberDebugPerformance(reporter = resolvedPerformanceReporter)
    val shellCrashReporter = rememberCrashReporter(override = crashReporter)
    val resolvedCrashReporter = remember(shellCrashReporter) {
        DeveloperObservability.wrapCrashReporter(shellCrashReporter)
    }

    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbarHostState,
        LocalSnackbarDispatcher provides snackbarDispatcher,
        LocalLoadingController provides loadingController,
        LocalDialogController provides dialogController,
    ) {
        PerformanceCompositionRoot(performance = performance) {
            AnalyticsCompositionRoot(analytics = resolvedAnalytics) {
                ExceptionMonitoringRoot(
                    snackbarDispatcher = snackbarDispatcher,
                    crashReporter = resolvedCrashReporter,
                ) {
                    BaseScaffoldContent(
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
private fun BaseScaffoldContent(
    modifier: Modifier,
    consumeStatusBarsInContent: Boolean,
    bottomBar: @Composable () -> Unit,
    floatingActionButton: @Composable () -> Unit,
    header: @Composable () -> Unit,
    contentModifier: Modifier,
    hosts: ScaffoldGlobalHosts,
    ownsGlobalHosts: Boolean,
    content: @Composable (PaddingValues) -> Unit,
) {
    val performance = rememberPerformance()
    val performanceContentModifier = PerformanceScaffoldBindings(
        performance = performance,
        contentModifier = contentModifier,
    )
    val pageBackground = PageBackground.color()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pageBackground),
    ) {
        MaterialScaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = pageBackground,
            contentWindowInsets = InsetsPolicy.scaffoldContentWindowInsets,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            snackbarHost = {
                if (ownsGlobalHosts) {
                    SnackbarHost(hostState = hosts.snackbarHostState)
                }
            },
        ) { innerPadding ->
            val contentInsets = rememberScaffoldContentPadding(scaffoldPadding = innerPadding)
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
            BlockingLoadingOverlay(
                visible = loadingState.visible,
                message = loadingState.message,
            )
            DialogHost(controller = hosts.dialogController)
        }
    }
}
