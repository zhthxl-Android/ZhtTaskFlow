package com.example.zhttaskflow.feature.task.domain.usecase

import com.example.zhttaskflow.feature.task.domain.TaskRepository

/**
 * 删除任务用例。
 *
 * **调用边界**：表现层确认删除任务时调用。
 */
class DeleteTaskUseCase(
    private val repository: TaskRepository,
) {

    suspend operator fun invoke(id: String) {
        repository.deleteTask(id)
    }
}
