package com.example.zhttaskflow.base.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zhttaskflow.base.foundation.TaskFlowLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI ViewModel 基类：聚合 [uiState] 与 [uiEffect]，统一协程异常日志。
 *
 * @param State 不可变页面状态
 * @param Event 用户事件
 * @param Effect 一次性副作用
 */
abstract class BaseViewModel<State : BaseUiState, Event : BaseUiEvent, Effect : BaseUiEffect>(
    initialState: State,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _uiEffect = Channel<Effect>(Channel.BUFFERED)
    val uiEffect: Flow<Effect> = _uiEffect.receiveAsFlow()

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
     * 发送一次性副作用，在 [viewModelScope] 内写入 Channel。
     */
    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }

    /**
     * 在 ViewModel 生命周期内执行挂起任务，统一捕获非取消异常并记录日志。
     */
    protected fun launchTask(
        tag: String = "BaseViewModel",
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                TaskFlowLogger.e(tag, throwable.message ?: "协程任务失败", throwable)
                onError?.invoke(throwable)
            }
        }
    }
}
