package com.example.zhttaskflow.feature.task.ui

import com.example.zhttaskflow.base.mvi.BaseUiEvent

/**
 * 任务列表页用户事件：页面唯一输入源。
 */
sealed interface TaskUiEvent : BaseUiEvent {

    /** 下拉刷新或重新拉取列表 */
    data object Refresh : TaskUiEvent

    /**
     * 新增任务。
     *
     * @param title 标题
     * @param content 内容
     */
    data class AddTask(
        val title: String,
        val content: String,
    ) : TaskUiEvent

    /**
     * 点击列表条目。
     *
     * @param taskId 任务 id
     */
    data class TaskItemClicked(
        val taskId: String,
    ) : TaskUiEvent
}
