package com.example.zhttaskflow.feature.home.presentation

import com.example.zhttaskflow.base.mvi.BaseUiEvent

/**
 * 首页用户事件（唯一 UI 输入）。
 */
sealed interface HomeUiEvent : BaseUiEvent {

    /**
     * 点击功能入口卡片。
     *
     * @param entranceId 入口标识，见 [com.example.zhttaskflow.feature.home.domain.HomeEntranceIds]
     */
    data class EntranceClicked(val entranceId: String) : HomeUiEvent

    /**
     * 空态/错误态重试（占位：后续接入首页加载 UseCase 后替换为真实刷新）。
     */
    data object Retry : HomeUiEvent
}
