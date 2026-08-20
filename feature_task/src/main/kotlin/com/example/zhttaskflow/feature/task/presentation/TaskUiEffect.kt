package com.example.zhttaskflow.feature.task.presentation

import com.example.zhttaskflow.base.mvi.BaseUiEffect

/**
 * 任务列表页一次性副作用：Toast、导航等由 UI 层消费，不写入 [TaskUiState]。
 */
sealed interface TaskUiEffect : BaseUiEffect {

    /**
     * 展示短提示。
     *
     * 触发场景：加载/刷新失败、新增校验、操作结果等需要轻提示的场景。
     */
    data class ShowToast(val message: String) : TaskUiEffect

    /**
     * 跳转任务编辑/详情页。
     *
     * @param url 完整 Navigation 路由 path（由 [com.example.zhttaskflow.feature.task.navigation.TaskRoute.detailPath] 生成）
     */
    data class NavigateToEdit(val url: String) : TaskUiEffect
}
