package com.example.zhttaskflow.feature.home.presentation

import com.example.zhttaskflow.base.mvi.BaseUiEffect

/**
 * 首页一次性副作用，不写入 [HomeUiState]。
 *
 * **消费分层**：
 * - 页面内 UI 反馈（如 Toast 等，若有）：由 [HomeScreen] 消费
 * - 跨页面导航（[NavigateToRoute]）：由 [com.example.zhttaskflow.feature.home.navigation.HomeRouteHost] 消费
 */
sealed interface HomeUiEffect : BaseUiEffect {

    /**
     * 跳转目标页面（跨页面导航）。
     *
     * @param url 完整 Navigation 路由 path（由宿主或各 Feature 路由常量生成）
     */
    data class NavigateToRoute(val url: String) : HomeUiEffect
}
