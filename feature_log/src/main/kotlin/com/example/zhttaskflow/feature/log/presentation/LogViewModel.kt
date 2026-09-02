package com.example.zhttaskflow.feature.log.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.mvi.BaseViewModel

/**
 * 日志查看 ViewModel：MVI 骨架，后续任务接入本地日志 UseCase。
 */
internal class LogViewModel :
    BaseViewModel<LogUiState, LogUiEvent, LogUiEffect>(BaseUiState.Empty) {

    override fun handleEvent(event: LogUiEvent) {
        when (event) {
            LogUiEvent.Retry -> {
                // 占位：接入日志加载用例后改为 launchTask { ... }
                setState { BaseUiState.Empty }
            }
        }
    }
}

internal typealias LogUiState = BaseUiState<LogData>

/**
 * [LogViewModel] 手动注入工厂（无 Hilt）。
 */
internal class LogViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogViewModel::class.java)) {
            return LogViewModel() as T
        }
        throw IllegalArgumentException("未知 ViewModel: ${modelClass.name}")
    }
}
