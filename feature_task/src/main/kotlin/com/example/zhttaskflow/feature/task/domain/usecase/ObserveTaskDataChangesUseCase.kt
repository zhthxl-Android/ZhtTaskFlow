package com.example.zhttaskflow.feature.task.domain.usecase

import com.example.zhttaskflow.feature.task.domain.TaskDataChanged
import com.example.zhttaskflow.feature.task.domain.TaskRepository
import kotlinx.coroutines.flow.Flow

/**
 * 订阅任务仓库数据变更事件，用于列表页在详情保存等场景下自动刷新。
 */
class ObserveTaskDataChangesUseCase(
    private val repository: TaskRepository,
) {

    operator fun invoke(): Flow<TaskDataChanged> = repository.observeTaskDataChanges()
}
