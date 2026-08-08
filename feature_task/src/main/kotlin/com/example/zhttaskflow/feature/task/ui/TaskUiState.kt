package com.example.zhttaskflow.feature.task.ui

import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.feature.task.domain.Task

/**
 * 任务列表页 UI 状态：驱动列表、加载、刷新与错误展示。
 */
data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
) : BaseUiState {

    /** 无数据且非加载中、无错误时的空态 */
    val isEmpty: Boolean =
        !isLoading && !isRefreshing && tasks.isEmpty() && errorMessage == null
}
