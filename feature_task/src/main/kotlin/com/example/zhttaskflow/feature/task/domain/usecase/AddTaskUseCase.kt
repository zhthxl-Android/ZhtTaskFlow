package com.example.zhttaskflow.feature.task.domain.usecase

import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskRepository

/**
 * 新增任务用例。
 *
 * **调用边界**：表现层提交新建任务时调用；不包含表单校验与列表刷新编排。
 */
class AddTaskUseCase(
    private val repository: TaskRepository,
) {

    suspend operator fun invoke(task: Task) {
        repository.addTask(task)
    }
}
