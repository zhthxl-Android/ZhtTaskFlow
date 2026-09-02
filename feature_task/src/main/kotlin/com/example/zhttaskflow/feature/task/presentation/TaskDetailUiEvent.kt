package com.example.zhttaskflow.feature.task.presentation

import com.example.zhttaskflow.base.mvi.BaseUiEvent
import com.example.zhttaskflow.feature.task.domain.TaskStatus

/**
 * 任务详情页用户事件。
 */
sealed interface TaskDetailUiEvent : BaseUiEvent {

    /** 首次进入或参数变化时加载详情 */
    data class Load(val taskId: String) : TaskDetailUiEvent

    /** StateBox 错误态重试 */
    data object Retry : TaskDetailUiEvent

    /** 进入编辑模式 */
    data object StartEdit : TaskDetailUiEvent

    /** 取消编辑并恢复草稿 */
    data object CancelEdit : TaskDetailUiEvent

    data class DraftTitleChanged(val value: String) : TaskDetailUiEvent

    data class DraftContentChanged(val value: String) : TaskDetailUiEvent

    /** 保存标题与内容 */
    data object SaveEdit : TaskDetailUiEvent

    /** 流转任务状态 */
    data class ChangeStatus(val status: TaskStatus) : TaskDetailUiEvent

    /** 点击附件条目 */
    data class AttachmentClicked(val attachmentId: String) : TaskDetailUiEvent
}
