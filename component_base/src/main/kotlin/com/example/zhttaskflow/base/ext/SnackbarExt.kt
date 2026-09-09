package com.example.zhttaskflow.base.ext

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import com.example.zhttaskflow.base.ui.SnackbarVisuals
import com.example.zhttaskflow.base.ui.defaultDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * MVI 层 Snackbar Effect 使用的语义类型（Success / Error / Normal）。
 *
 * 与展示层 [com.example.zhttaskflow.base.ui.SnackbarType] 一一对应，业务模块依赖本类型即可，无需直接引用 Material Snackbar API。
 */
typealias SnackbarType = com.example.zhttaskflow.base.ui.SnackbarType

/**
 * 由 [com.example.zhttaskflow.base.ui.BaseScaffold] 注入的 Snackbar 宿主状态。
 */
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("SnackbarHostState 未提供，请使用 BaseScaffold 包裹页面")
}

/**
 * 非挂起场景下一行调用的 Snackbar 调度器（内部串行队列，避免多条叠加）。
 */
val LocalSnackbarDispatcher = compositionLocalOf<SnackbarDispatcher> {
    error("SnackbarDispatcher 未提供，请使用 BaseScaffold 包裹页面")
}

/**
 * 在 Composable 中获取调度器：`val snackbar = LocalSnackbarDispatcher.current`。
 */
@Stable
class SnackbarDispatcher internal constructor(
    private val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    private val queueMutex = Mutex()

    /**
     * 展示 Snackbar（非 suspend，适用于 onClick 等回调）。
     */
    fun showSnackbar(
        message: String,
        type: SnackbarType = SnackbarType.Normal,
        duration: SnackbarDuration? = null,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        onAction: (() -> Unit)? = null,
    ) {
        scope.launch {
            queueMutex.withLock {
                hostState.showSnackbar(
                    message = message,
                    type = type,
                    duration = duration,
                    actionLabel = actionLabel,
                    withDismissAction = withDismissAction,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
fun rememberSnackbarDispatcher(): SnackbarDispatcher {
    return LocalSnackbarDispatcher.current
}

/**
 * 挂起展示 Snackbar，支持预设类型、自定义时长与操作按钮。
 */
suspend fun SnackbarHostState.showSnackbar(
    message: String,
    type: SnackbarType = SnackbarType.Normal,
    duration: SnackbarDuration? = null,
    actionLabel: String? = null,
    withDismissAction: Boolean = false,
    onAction: (() -> Unit)? = null,
): SnackbarResult {
    val visuals = SnackbarVisuals(
        message = message,
        type = type,
        actionLabel = actionLabel,
        withDismissAction = withDismissAction,
        duration = duration ?: type.defaultDuration,
    )
    val result = showSnackbar(visuals)
    if (result == SnackbarResult.ActionPerformed) {
        onAction?.invoke()
    }
    return result
}

/**
 * 顶层便捷方法：一行展示 Snackbar（需传入 [SnackbarDispatcher]，通常来自 [LocalSnackbarDispatcher]）。
 */
fun showSnackbar(
    dispatcher: SnackbarDispatcher,
    message: String,
    type: SnackbarType = SnackbarType.Normal,
    duration: SnackbarDuration? = null,
    actionLabel: String? = null,
    withDismissAction: Boolean = false,
    onAction: (() -> Unit)? = null,
) {
    dispatcher.showSnackbar(
        message = message,
        type = type,
        duration = duration,
        actionLabel = actionLabel,
        withDismissAction = withDismissAction,
        onAction = onAction,
    )
}
