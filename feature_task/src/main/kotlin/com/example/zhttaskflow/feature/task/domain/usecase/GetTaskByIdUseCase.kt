package com.example.zhttaskflow.feature.task.domain.usecase

import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskRepository

/**
 * 按 id 查询任务详情用例。
 */
class GetTaskByIdUseCase(
    private val repository: TaskRepository,
) {

  /**
   * @return 存在则返回 [Task]，否则 `null`
   */
    suspend operator fun invoke(taskId: String): Task? {
        return repository.getTaskById(taskId)
    }
}
