package com.example.zhttaskflow.feature.task.domain

/**
 * 任务领域实体：不可变数据结构，仅承载业务字段，不包含行为逻辑。
 *
 * @param id 任务唯一标识
 * @param title 任务标题
 * @param content 任务内容描述
 * @param createdAt 创建时间（毫秒时间戳，UTC 由调用方约定）
 * @param status 任务状态
 */
data class Task(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val status: TaskStatus,
)
