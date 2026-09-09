package com.example.zhttaskflow.feature.task.presentation

import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.ext.NavigationUiEffect
import com.example.zhttaskflow.base.ext.PresentationUiEffect
import com.example.zhttaskflow.base.ext.UiEffectConsumption
import com.example.zhttaskflow.base.mvi.BaseUiEffect

/**
 * 任务列表页一次性副作用，不写入 [TaskUiState]。
 *
 * 双 Collector 规范见 [UiEffectConsumption]。
 */
sealed interface TaskUiEffect : BaseUiEffect {

    /**
     * 展示全局 Snackbar（页面内 UI 反馈）。
     *
     * @param message 用户可读文案
     * @param type 成功 / 错误 / 普通样式，由业务层指定
     */
    data class ShowSnackbar(
        val message: String,
        val type: SnackbarType = SnackbarType.Normal,
    ) : TaskUiEffect, PresentationUiEffect

    /**
     * 跳转任务编辑/详情页（跨页面导航）。
     *
     * @param url 完整 Navigation 路由 path（由 [com.example.zhttaskflow.nav.route.TaskNavRoutes.detailPath] 生成）
     */
    data class NavigateToEdit(val url: String) : TaskUiEffect, NavigationUiEffect
}
