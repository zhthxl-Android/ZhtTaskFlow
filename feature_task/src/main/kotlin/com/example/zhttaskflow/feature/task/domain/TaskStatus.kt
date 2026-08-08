package com.example.zhttaskflow.feature.task.domain

/**
 * 任务业务状态枚举，表达领域内的生命周期语义。
 */
enum class TaskStatus {
    /** 待处理 */
    PENDING,

    /** 进行中 */
    IN_PROGRESS,

    /** 已完成 */
    COMPLETED,

    /** 已取消 */
    CANCELLED,
}
