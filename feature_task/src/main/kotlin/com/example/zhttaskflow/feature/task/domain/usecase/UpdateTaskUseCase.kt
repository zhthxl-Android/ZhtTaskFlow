package com.example.zhttaskflow.feature.task.domain.usecase

import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskRepository

/**
 * 更新任务用例。
 *
 * **调用边界**：表现层或详情页提交任务变更时调用。
 */
class UpdateTaskUseCase(
    private val repository: TaskRepository,
) {

    suspend operator fun invoke(task: Task) {
        repository.updateTask(task)
    }
}
