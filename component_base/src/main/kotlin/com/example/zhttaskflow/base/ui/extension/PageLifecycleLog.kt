package com.example.zhttaskflow.base.ui.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.example.zhttaskflow.base.analytics.TaskFlowPageViewEvent
import com.example.zhttaskflow.base.analytics.rememberTaskFlowAnalytics
import com.example.zhttaskflow.base.performance.rememberTaskFlowPerformance

private const val PAGE_LIFECYCLE_LOG_TAG = "PageLifecycle"

/**
 * 页面生命周期 Debug 日志：仅在进入、退出、参数变化时输出，与重组解耦。
 *
 * 底层经 [com.example.zhttaskflow.base.analytics.TaskFlowAnalytics.trackPageView] /
 * [com.example.zhttaskflow.base.analytics.TaskFlowAnalytics.trackPageLeave] 上报，默认调试实现与改造前 Logcat 一致。
 *
 * @param pageName 页面标识（路由名、Screen 名等）
 * @param pageArgs 可选参数字符串（用于跳转追溯）
 * @param tag 保留参数，兼容历史签名；调试 Tag 由 [com.example.zhttaskflow.base.analytics.TaskFlowDebugAnalytics] 固定为 `PageLifecycle`
 * @param onEnter 进入 composition 时回调（日志之后）
 * @param onLeave 离开 composition 时回调（日志之后）
 * @param onArgsChange 参数变化时回调（不含首次进入，避免与 onEnter 重复）
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun PageLifecycleLog(
    pageName: String,
    pageArgs: String? = null,
    tag: String = PAGE_LIFECYCLE_LOG_TAG,
    onEnter: () -> Unit = {},
    onLeave: () -> Unit = {},
    onArgsChange: (String?) -> Unit = {},
) {
    val analytics = rememberTaskFlowAnalytics()
    val performance = rememberTaskFlowPerformance()
    val argsState = rememberUpdatedState(pageArgs)
    var argsEffectInitialized by remember(pageName) { mutableStateOf(false) }

    DisposableEffect(pageName) {
        performance.beginPage(pageName)
        analytics.trackPageView(
            pageId = pageName,
            pageArgs = argsState.value,
            event = TaskFlowPageViewEvent.Enter,
        )
        onEnter()
        onDispose {
            performance.endPage(pageName)
            analytics.trackPageLeave(pageId = pageName)
            onLeave()
        }
    }

    LaunchedEffect(pageArgs) {
        if (!argsEffectInitialized) {
            argsEffectInitialized = true
            return@LaunchedEffect
        }
        analytics.trackPageView(
            pageId = pageName,
            pageArgs = pageArgs,
            event = TaskFlowPageViewEvent.ArgsChange,
        )
        onArgsChange(pageArgs)
    }
}
