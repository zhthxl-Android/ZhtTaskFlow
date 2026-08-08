package com.example.zhttaskflow.feature.task.ui

import com.example.zhttaskflow.base.mvi.BaseUiEffect

/**
 * 任务列表页一次性副作用：Toast 等 UI 层消费后不再重放。
 *
 * 页面跳转由 [TaskViewModel] 通过 [com.example.zhttaskflow.nav.TaskFlowNavigator] 直接触发，不作为 Effect 下发。
 */
sealed interface TaskUiEffect : BaseUiEffect {

    /** 展示短提示 */
    data class ShowToast(val message: String) : TaskUiEffect
}
