package com.example.zhttaskflow.base.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.zhttaskflow.base.analytics.Analytics
import com.example.zhttaskflow.base.analytics.AnalyticsCompositionRoot
import com.example.zhttaskflow.base.analytics.rememberDebugAnalytics
import com.example.zhttaskflow.base.exception.CrashReporter
import com.example.zhttaskflow.base.exception.CrashReporterCompositionRoot
import com.example.zhttaskflow.base.exception.rememberCrashReporter
import com.example.zhttaskflow.base.ext.DialogController
import com.example.zhttaskflow.base.ext.LoadingController
import com.example.zhttaskflow.base.ext.LocalDialogController
import com.example.zhttaskflow.base.ext.LocalLoadingController
import com.example.zhttaskflow.base.ext.LocalSnackbarDispatcher
import com.example.zhttaskflow.base.ext.LocalSnackbarHostState
import com.example.zhttaskflow.base.ext.SnackbarDispatcher
import com.example.zhttaskflow.base.observability.DeveloperObservability
import com.example.zhttaskflow.base.performance.DebugPerformanceReporter
import com.example.zhttaskflow.base.performance.PerformanceCompositionRoot
import com.example.zhttaskflow.base.performance.PerformanceReporter
import com.example.zhttaskflow.base.performance.rememberDebugPerformance

@Composable
internal fun ScaffoldProviderRoot(
    analytics: Analytics? = null,
    performanceImpl: PerformanceReporter? = null,
    crashReporter: CrashReporter? = null,
    content: @Composable (hosts: ScaffoldGlobalHosts) -> Unit,
) {
    //交互宿主初始化
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val snackbarDispatcher = remember(
        snackbarHostState,
        snackbarScope
    ) {
        SnackbarDispatcher(
            hostState = snackbarHostState,
            scope = snackbarScope,
        )
    }
    val loadingController = remember { LoadingController() }
    val dialogController = remember { DialogController() }
    //4 个实例打包
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
    //交互宿主生命周期管理
    DisposableEffect(loadingController) {
        onDispose { loadingController.hideLoading() }
    }
    DisposableEffect(dialogController) {
        onDispose { dialogController.dismissAll() }
    }
    //监控能力注入
    //埋点
    val shellAnalytics = analytics ?: rememberDebugAnalytics()
    val resolvedAnalytics = remember(shellAnalytics) {
        DeveloperObservability.wrapAnalytics(shellAnalytics)
    }
    //性能
    val shellPerformanceReporter = performanceImpl ?: DebugPerformanceReporter
    val resolvedPerformanceReporter = remember(shellPerformanceReporter) {
        DeveloperObservability.wrapPerformanceReporter(shellPerformanceReporter)
    }
    val performance = rememberDebugPerformance(reporter = resolvedPerformanceReporter)
    //崩溃
    val shellCrashReporter = rememberCrashReporter(override = crashReporter)
    val resolvedCrashReporter = remember(shellCrashReporter) {
        DeveloperObservability.wrapCrashReporter(shellCrashReporter)
    }
    //全局注入与嵌套
    CompositionLocalProvider(
        LocalSnackbarHostState provides snackbarHostState,
        LocalSnackbarDispatcher provides snackbarDispatcher,
        LocalLoadingController provides loadingController,
        LocalDialogController provides dialogController,
    ) {
        //将 性能监控上报器 实例注入到整个 Compose 树
        PerformanceCompositionRoot(performance = performance) {
            //将 埋点监控上报器 实例注入到整个 Compose 树
            AnalyticsCompositionRoot(analytics = resolvedAnalytics) {
                //将 崩溃监控上报器 实例注入到整个 Compose 树
                CrashReporterCompositionRoot(crashReporter = resolvedCrashReporter) {
                    //并添加网络断开横幅
                    NetworkMonitoringRoot {
                        content(localHosts)
                    }
                }

            }
        }
    }
}