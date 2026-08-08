package com.example.zhttaskflow.feature.task.data

import com.example.zhttaskflow.base.foundation.TaskFlowIllegalStateException
import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 任务内存模拟数据源：提供增删改查能力，进程内单例存储。
 *
 * 扩展入口：后续可引入 Room，将实现迁移至 `TaskRoomDataSource` 并在此类或工厂处切换数据源，
 * 对外仍通过 [TaskRepositoryImpl] 暴露 [com.example.zhttaskflow.feature.task.domain.TaskRepository]。
 */
class TaskMockDataSource {

    private val mutex = Mutex()
    private val taskStore = mutableMapOf<String, Task>()

    /**
     * 查询全部任务（按创建时间倒序）。
     */
    suspend fun getAll(): List<Task> = mutex.withLock {
        taskStore.values.sortedByDescending { it.createdAt }
    }

    /**
     * 按 id 查询任务。
     */
    suspend fun getById(id: String): Task? = mutex.withLock {
        taskStore[id]
    }

    /**
     * 新增任务；若 id 已存在则抛出 [TaskFlowIllegalStateException]。
     */
    suspend fun insert(task: Task) = mutex.withLock {
        if (taskStore.containsKey(task.id)) {
            throw TaskFlowIllegalStateException("任务 id 已存在: ${task.id}")
        }
        taskStore[task.id] = task
    }

    /**
     * 更新任务；若 id 不存在则抛出 [TaskFlowIllegalStateException]。
     */
    suspend fun update(task: Task) = mutex.withLock {
        if (!taskStore.containsKey(task.id)) {
            throw TaskFlowIllegalStateException("任务不存在，无法更新: ${task.id}")
        }
        taskStore[task.id] = task
    }

    /**
     * 删除任务；若 id 不存在则抛出 [TaskFlowIllegalStateException]。
     */
    suspend fun delete(id: String) = mutex.withLock {
        if (taskStore.remove(id) == null) {
            throw TaskFlowIllegalStateException("任务不存在，无法删除: $id")
        }
    }

    /**
     * 写入演示种子数据（可选，便于联调列表页）。
     */
    suspend fun seedDemoTasksIfEmpty() = mutex.withLock {
        if (taskStore.isNotEmpty()) {
            return
        }
        val now = System.currentTimeMillis()
        taskStore["demo-1"] = Task(
            id = "demo-1",
            title = "示例任务",
            content = "这是一条内存模拟任务",
            createdAt = now,
            status = TaskStatus.PENDING,
        )
    }
}
