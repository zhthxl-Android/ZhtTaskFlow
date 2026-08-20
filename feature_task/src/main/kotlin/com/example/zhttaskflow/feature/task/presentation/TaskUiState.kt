package com.example.zhttaskflow.feature.task.presentation

import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.feature.task.domain.Task

/**
 * 任务列表页业务载荷（仅出现在 [BaseUiState.Success] 中）。
 */
data class TaskListData(
    val tasks: List<Task> = emptyList(),
    val isRefreshing: Boolean = false,
)

/** 任务列表页 UI 状态：`BaseUiState` 通用分支 + [TaskListData] 业务数据。 */
typealias TaskUiState = BaseUiState<TaskListData>
