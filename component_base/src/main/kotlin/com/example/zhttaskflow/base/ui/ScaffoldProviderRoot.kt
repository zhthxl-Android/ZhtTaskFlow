package com.example.zhttaskflow.base.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.example.zhttaskflow.base.analytics.Analytics
import com.example.zhttaskflow.base.analytics.LocalAnalytics
import com.example.zhttaskflow.base.analytics.rememberManagedAnalytics
import com.example.zhttaskflow.base.exception.CrashReporter
import com.example.zhttaskflow.base.exception.LocalCrashReporter
import com.example.zhttaskflow.base.exception.rememberManagedCrashReporter
import com.example.zhttaskflow.base.ext.DialogController
import com.example.zhttaskflow.base.ext.LoadingController
import com.example.zhttaskflow.base.ext.LocalDialogController
import com.example.zhttaskflow.base.ext.LocalLoadingController
import com.example.zhttaskflow.base.ext.LocalSnackbarDispatcher
import com.example.zhttaskflow.base.ext.LocalSnackbarHostState
import com.example.zhttaskflow.base.ext.SnackbarDispatcher
import com.example.zhttaskflow.base.performance.LocalPerformance
import com.example.zhttaskflow.base.performance.PerformanceReporter
import com.example.zhttaskflow.base.performance.rememberManagedPerformance

/**
 * 最外层脚手架专用的基础设施注入根
 * 内层脚手架复用父级宿主时不会调用它
 * */
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
    val resolvedAnalytics = rememberManagedAnalytics(analytics)
    //性能
    val resolvedPerformance = rememberManagedPerformance(performanceImpl)
    //崩溃
    val resolvedCrashReporter = rememberManagedCrashReporter(crashReporter)
    //全局注入与嵌套
    CompositionLocalProvider(
        // 交互宿主 4 个
        LocalSnackbarHostState provides snackbarHostState,
        LocalSnackbarDispatcher provides snackbarDispatcher,
        LocalLoadingController provides loadingController,
        LocalDialogController provides dialogController,
        // 监控能力 3 个
        LocalAnalytics provides resolvedAnalytics,
        LocalPerformance provides resolvedPerformance,
        LocalCrashReporter provides resolvedCrashReporter
    ) {
        //并添加网络断开横幅
        NetworkMonitoringRoot {
            content(localHosts)
        }
    }
}