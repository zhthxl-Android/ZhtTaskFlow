package com.example.zhttaskflow.feature.task.data

import com.example.zhttaskflow.core.network.ApiResult
import com.example.zhttaskflow.core.network.safeApiCall
import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskDataChanged
import com.example.zhttaskflow.feature.task.domain.TaskRepository
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * [TaskRepository] 实现：委托 [TaskMockDataSource] 完成持久化，内部统一走 [safeApiCall] 与 [ApiResult] 范式。
 *
 * 无 DI 框架：由 presentation 或 Application 手动构造并持有本类实例。
 */
class TaskRepositoryImpl(
    private val dataSource: TaskMockDataSource,
) : TaskRepository {

    private val taskDataChanges = MutableSharedFlow<TaskDataChanged>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override fun observeTaskDataChanges(): Flow<TaskDataChanged> = taskDataChanges.asSharedFlow()

    override suspend fun getTaskById(id: String): Task? {
        return unwrapOrThrow(safeApiCall {
            dataSource.seedDemoTasksIfEmpty()
            dataSource.getById(id)
        })
    }

    override suspend fun getAllTasks(): List<Task> {
        return unwrapOrThrow(safeApiCall {
            dataSource.seedDemoTasksIfEmpty()
            dataSource.getAll()
        })
    }

    override suspend fun addTask(task: Task) {
        unwrapOrThrow(safeApiCall { dataSource.insert(task) })
        emitTaskDataChanged()
    }

    override suspend fun updateTask(task: Task) {
        unwrapOrThrow(safeApiCall { dataSource.update(task) })
        emitTaskDataChanged()
    }

    override suspend fun deleteTask(id: String) {
        unwrapOrThrow(safeApiCall { dataSource.delete(id) })
        emitTaskDataChanged()
    }

    private suspend fun emitTaskDataChanged() {
        taskDataChanges.emit(TaskDataChanged)
    }

    /**
     * 将 [ApiResult] 转为业务返回值；失败时向上抛出，由 ViewModel 统一记录 Error 日志。
     */
    private fun <T> unwrapOrThrow(result: ApiResult<T>): T {
        return when (result) {
            is ApiResult.Success -> result.data
            is ApiResult.Failure -> throw result.exception
        }
    }
}
