package com.example.zhttaskflow.feature.task.ui

import com.example.zhttaskflow.base.mvi.BaseUiEffect

/**
 * 任务列表页一次性副作用：Toast、导航等，由 UI 层消费后不再重放。
 */
sealed interface TaskUiEffect : BaseUiEffect {

    /** 展示短提示 */
    data class ShowToast(val message: String) : TaskUiEffect

    /** 跳转任务详情（路由由 UI 层对接 component_nav） */
    data class NavigateToTaskDetail(val taskId: String) : TaskUiEffect
}
