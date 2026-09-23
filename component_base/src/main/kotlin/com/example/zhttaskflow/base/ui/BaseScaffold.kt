package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.zhttaskflow.base.analytics.Analytics
import com.example.zhttaskflow.base.exception.CrashReporter
import com.example.zhttaskflow.base.exception.DebugCrashReporter
import com.example.zhttaskflow.base.exception.LocalCrashReporter
import com.example.zhttaskflow.base.ext.DialogController
import com.example.zhttaskflow.base.ext.LoadingController
import com.example.zhttaskflow.base.ext.LocalDialogController
import com.example.zhttaskflow.base.ext.LocalLoadingController
import com.example.zhttaskflow.base.ext.LocalSnackbarDispatcher
import com.example.zhttaskflow.base.ext.LocalSnackbarHostState
import com.example.zhttaskflow.base.ext.SnackbarDispatcher
import com.example.zhttaskflow.base.performance.DebugPerformanceReporter
import com.example.zhttaskflow.base.performance.PerformanceReporter
import com.example.zhttaskflow.base.performance.PerformanceScaffoldBindings
import com.example.zhttaskflow.base.performance.rememberPerformance
import com.example.zhttaskflow.base.ui.dialog.DialogHost
import androidx.compose.material3.Scaffold as MaterialScaffold

/**
 * 壳层/页面层共用的全局交互宿主（Snackbar / Loading / Dialog）。
 * 4 个全局交互控制器 / 状态打包成一个数据载体，方便父子脚手架之间批量传递，避免逐个判断和传递 4 个对象
 */
internal data class ScaffoldGlobalHosts(
    //官方提供的 Snackbar 宿主状态类，管理 Snackbar 的显示队列、生命周期，提供 showSnackbar 挂起方法
    val snackbarHostState: SnackbarHostState,
    //自定义封装的 Snackbar 分发器
    val snackbarDispatcher: SnackbarDispatcher,
    //自定义全局加载控制器，管理全屏阻塞加载的显示 / 隐藏状态、加载文案
    val loadingController: LoadingController,
    //自定义全局对话框控制器，管理全局 Dialog 的显示队列、销毁逻辑
    val dialogController: DialogController,
)

/**
 * 安全检测当前组合树中是否存在父级脚手架注入的全局宿主
 * 若组合树上游已由 [BaseScaffold] 注入全局宿主，
 * 如果完整存在则返回封装对象，任意一个缺失则返回 `null`
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
 * 并与 [com.example.zhttaskflow.base.ui.extension.PageLifecycleAnalytics] 的 `pageName` 关联。
 * @see com.example.zhttaskflow.base.doc.BaseArchitecture
 */
@Composable
fun BaseScaffold(
    modifier: Modifier = Modifier,
    //控制内容区是否避让状态栏,true 避让
    consumeStatusBarsInContent: Boolean,
    //埋点实现注入
    analytics: Analytics? = null,
    //性能实现注入
    performanceImpl: PerformanceReporter? = null,
    //崩溃上报实现注入
    crashReporter: CrashReporter? = null,
    //底部栏槽位
    bottomBar: @Composable () -> Unit = {},
    //悬浮按钮槽位
    floatingActionButton: @Composable () -> Unit = {},
    //顶部栏槽位
    header: @Composable () -> Unit = {},
    //内容区域的额外修饰符
    contentModifier: Modifier = Modifier,
    //内容区槽位
    content: @Composable (scaffoldContentPadding: PaddingValues) -> Unit,
) {
    val parentHosts = parentGlobalHostsOrNull()
    //存在父级宿主
    if (parentHosts != null) {
        //直接复用父级的所有全局宿主，不重复创建任何控制器、不重复注入监控能力
        BaseScaffoldContent(
            modifier = modifier,
            consumeStatusBarsInContent = consumeStatusBarsInContent,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            header = header,
            contentModifier = contentModifier,
            hosts = parentHosts,
            ownsGlobalHosts = false,//已经有宿主，设为false
            content = content,
        )
        return
    }
    //不存在父级宿主
    ScaffoldProviderRoot(
        analytics = analytics,
        performanceImpl = performanceImpl,
        crashReporter = crashReporter
    ) { localHosts ->
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
    //给页面内容 Modifier 挂载滚动监听
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
