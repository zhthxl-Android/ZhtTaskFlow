package com.example.zhttaskflow.feature.task.presentation

import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.feature.task.domain.Task

/**
 * 任务详情页业务载荷（仅出现在 [BaseUiState.Success] 中）。
 */
data class TaskDetailData(
    val task: Task,
    val isEditing: Boolean = false,
    val draftTitle: String = task.title,
    val draftContent: String = task.content,
    val isSubmitting: Boolean = false,
)

/** 任务详情页 UI 状态。 */
typealias TaskDetailUiState = BaseUiState<TaskDetailData>
