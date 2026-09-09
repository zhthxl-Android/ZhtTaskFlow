package com.example.zhttaskflow.base.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zhttaskflow.core.foundation.userDisplayMessage
import com.example.zhttaskflow.core.log.Logger
import com.example.zhttaskflow.core.network.NetworkChecker
import com.example.zhttaskflow.core.util.nullIfBlank
import com.example.zhttaskflow.core.util.orEmpty
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI ViewModel 基类：聚合 [uiState] 与 [uiEffect]，统一协程异常日志。
 *
 * 四件套约定：[BaseUiState]（泛型密封态 + 内层业务 data）、[BaseUiEvent]、[BaseUiEffect]、本类。
 *
 * @param State 页面状态，通常为 `BaseUiState<FeatureListData>`
 * @param Event 用户事件
 * @param Effect 一次性副作用；子类型应实现 [com.example.zhttaskflow.base.ext.NavigationUiEffect] 或
 * [com.example.zhttaskflow.base.ext.PresentationUiEffect]，见 [com.example.zhttaskflow.base.ext.UiEffectConsumption]
 */
abstract class BaseViewModel<State : BaseUiState<*>, Event : BaseUiEvent, Effect : BaseUiEffect>(
    initialState: State,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<Effect>(
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val uiEffect: SharedFlow<Effect> = _uiEffect.asSharedFlow()

    /** 当前快照，供子类读取 */
    protected val currentState: State
        get() = _uiState.value

    /**
     * 页面唯一事件入口，驱动 [handleEvent]。
     */
    fun onEvent(event: Event) {
        handleEvent(event)
    }

    protected abstract fun handleEvent(event: Event)

    protected fun setState(reducer: State.() -> State) {
        _uiState.update(reducer)
    }

    /**
     * 发送一次性副作用，在 [viewModelScope] 内写入 SharedFlow（支持多订阅方分别消费）。
     */
    protected fun sendEffect(effect: Effect) {
        _uiEffect.tryEmit(effect)
    }

    /**
     * 提取面向用户的友好文案：优先网络层 [com.example.zhttaskflow.core.foundation.NetworkException.userMessage]。
     */
    protected fun getUserFriendlyMessage(
        throwable: Throwable,
        fallback: String = DEFAULT_USER_MESSAGE_FALLBACK,
    ): String = throwable.userDisplayMessage(fallback)

    /**
     * 在 ViewModel 生命周期内执行挂起任务，统一捕获非取消异常并记录日志。
     *
     * @param precheckNetwork 是否在执行 [block] 前做同步强无网预检；默认 `true`，无网时直接 [onError] 且不启动协程
     * @param userMessageFallback [onError] 第二参数在无 [userMessage] 时的兜底文案
     * @param onError 失败回调：`userMessage` 为友好文案，可直接用于 Error 态 / Snackbar；日志仍使用原始 [Throwable.message]
     */
    protected fun launchTask(
        tag: String = "BaseViewModel",
        scene: String? = null,
        precheckNetwork: Boolean = true,
        userMessageFallback: String = DEFAULT_USER_MESSAGE_FALLBACK,
        onError: ((throwable: Throwable, userMessage: String) -> Unit)? = null,
        block: suspend () -> Unit,
    ) {
        if (precheckNetwork && !NetworkChecker.isNetworkConnected()) {
            val networkError = NetworkChecker.unavailableNetworkException()
            val userMessage = getUserFriendlyMessage(networkError, userMessageFallback)
            logLaunchTaskFailure(tag, scene, networkError)
            onError?.invoke(networkError, userMessage)
            return
        }
        viewModelScope.launch {
            try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                val userMessage = getUserFriendlyMessage(throwable, userMessageFallback)
                logLaunchTaskFailure(tag, scene, throwable)
                onError?.invoke(throwable, userMessage)
            }
        }
    }

    private fun logLaunchTaskFailure(tag: String, scene: String?, throwable: Throwable) {
        val scenePrefix = scene?.let { "[$it] " }.orEmpty()
        Logger.errorAlways(
            tag,
            { "$scenePrefix${throwable.message.nullIfBlank() ?: "协程任务失败"}" },
            throwable,
        )
    }

    companion object {
        const val DEFAULT_USER_MESSAGE_FALLBACK: String = "加载失败，请稍后重试"
    }
}
