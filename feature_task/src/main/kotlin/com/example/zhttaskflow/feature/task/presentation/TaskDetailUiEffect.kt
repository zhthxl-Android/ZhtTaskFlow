package com.example.zhttaskflow.feature.task.presentation

import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.ext.PresentationUiEffect
import com.example.zhttaskflow.base.mvi.BaseUiEffect

/**
 * 任务详情页一次性副作用。
 */
sealed interface TaskDetailUiEffect : BaseUiEffect {

    data class ShowSnackbar(
        val message: String,
        val type: SnackbarType = SnackbarType.Normal,
        val actionId: String = "task_detail_snackbar",
    ) : TaskDetailUiEffect, PresentationUiEffect
}
