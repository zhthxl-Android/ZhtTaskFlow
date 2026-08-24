package com.example.zhttaskflow.base.ui.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.example.zhttaskflow.core.log.TaskFlowLogger
import com.example.zhttaskflow.core.util.orEmpty

private const val PAGE_LIFECYCLE_LOG_TAG = "PageLifecycle"

/**
 * 页面生命周期 Debug 日志：仅在进入、退出、参数变化时输出，与重组解耦。
 *
 * @param pageName 页面标识（路由名、Screen 名等）
 * @param pageArgs 可选参数字符串（用于跳转追溯）
 * @param tag 日志 Tag 后缀（完整为 `TaskFlow/{tag}`）
 * @param onEnter 进入 composition 时回调（日志之后）
 * @param onLeave 离开 composition 时回调（日志之后）
 * @param onArgsChange 参数变化时回调（不含首次进入，避免与 onEnter 重复）
 */
@Composable
fun PageLifecycleLog(
    pageName: String,
    pageArgs: String? = null,
    tag: String = PAGE_LIFECYCLE_LOG_TAG,
    onEnter: () -> Unit = {},
    onLeave: () -> Unit = {},
    onArgsChange: (String?) -> Unit = {},
) {
    val argsState = rememberUpdatedState(pageArgs)
    var argsEffectInitialized by remember(pageName) { mutableStateOf(false) }

    DisposableEffect(pageName) {
        TaskFlowLogger.d(tag) {
            "onEnter page=$pageName args=${argsState.value.orEmpty()}"
        }
        onEnter()
        onDispose {
            TaskFlowLogger.d(tag) { "onLeave page=$pageName" }
            onLeave()
        }
    }

    LaunchedEffect(pageArgs) {
        if (!argsEffectInitialized) {
            argsEffectInitialized = true
            return@LaunchedEffect
        }
        TaskFlowLogger.d(tag) {
            "onArgsChange page=$pageName args=${pageArgs.orEmpty()}"
        }
        onArgsChange(pageArgs)
    }
}
