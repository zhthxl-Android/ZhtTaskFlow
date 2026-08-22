package com.example.zhttaskflow.feature.task.presentation

import com.example.zhttaskflow.base.mvi.BaseUiEffect

/**
 * 任务列表页一次性副作用，不写入 [TaskUiState]。
 *
 * **消费分层**：
 * - 页面内 UI 反馈（如 [ShowToast]）：由 [TaskListScreen] 消费
 * - 跨页面导航（[NavigateToEdit]）：由 [com.example.zhttaskflow.feature.task.navigation.TaskListRouteHost] 消费
 */
sealed interface TaskUiEffect : BaseUiEffect {

    /**
     * 展示短提示（页面内 UI 反馈）。
     *
     * 触发场景：加载/刷新失败、新增校验、操作结果等需要轻提示的场景。
     */
    data class ShowToast(val message: String) : TaskUiEffect

    /**
     * 跳转任务编辑/详情页（跨页面导航）。
     *
     * @param url 完整 Navigation 路由 path（由 [com.example.zhttaskflow.nav.route.TaskFlowTaskNavRoutes.detailPath] 生成）
     */
    data class NavigateToEdit(val url: String) : TaskUiEffect
}
