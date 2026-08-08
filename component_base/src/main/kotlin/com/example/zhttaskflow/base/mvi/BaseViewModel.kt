package com.example.zhttaskflow.base.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI ViewModel 基类：统一 State / Event / Effect 单向数据流。
 *
 * @param State 页面状态，须实现 [BaseUiState]
 * @param Event 页面事件，须实现 [BaseUiEvent]
 * @param Effect 一次性副作用，须实现 [BaseUiEffect]
 */
abstract class BaseViewModel<State : BaseUiState, Event : BaseUiEvent, Effect : BaseUiEffect> : ViewModel() {

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _uiEffect = Channel<Effect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    /** 子类提供初始 UiState */
    protected abstract fun initialState(): State

  /** 子类处理用户事件，更新状态或发送副作用 */
    protected abstract fun onEvent(event: Event)

    /** 对外唯一事件入口 */
    fun sendEvent(event: Event) {
        onEvent(event)
    }

    protected fun updateState(reducer: (State) -> State) {
        _uiState.update(reducer)
    }

    protected fun currentState(): State = _uiState.value

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            _uiEffect.send(effect)
        }
    }
}
