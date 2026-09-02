package com.example.zhttaskflow.feature.log.presentation

import com.example.zhttaskflow.base.mvi.BaseUiEvent

/**
 * 日志查看页用户事件（唯一 UI 输入）。
 */
sealed interface LogUiEvent : BaseUiEvent {

    /** 占位：后续接入日志加载 UseCase 后用于重试。 */
    data object Retry : LogUiEvent
}
