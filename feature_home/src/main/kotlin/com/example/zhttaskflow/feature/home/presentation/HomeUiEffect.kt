package com.example.zhttaskflow.feature.home.presentation

import com.example.zhttaskflow.base.mvi.BaseUiEffect

/**
 * 首页一次性副作用：导航等由 UI 层消费，不写入 [HomeUiState]。
 */
sealed interface HomeUiEffect : BaseUiEffect {

    /**
     * 跳转目标页面。
     *
     * @param url 完整 Navigation 路由 path（由宿主或各 Feature 路由常量生成）
     */
    data class NavigateToRoute(val url: String) : HomeUiEffect
}
