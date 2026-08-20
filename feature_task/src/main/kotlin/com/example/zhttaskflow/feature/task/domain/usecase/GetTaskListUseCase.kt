package com.example.zhttaskflow.feature.task.domain.usecase

import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskRepository

/**
 * 获取全量任务列表用例。
 *
 * **调用边界**：表现层列表加载、刷新后重拉列表时调用。
 */
class GetTaskListUseCase(
    private val repository: TaskRepository,
) {

    suspend operator fun invoke(): List<Task> {
        return repository.getAllTasks()
    }
}
