package com.example.zhttaskflow.feature.task.data

import com.example.zhttaskflow.core.foundation.TaskFlowIllegalStateException
import com.example.zhttaskflow.core.log.TaskFlowLogger
import com.example.zhttaskflow.feature.task.domain.Task
import com.example.zhttaskflow.feature.task.domain.TaskStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val TASK_DATA_SOURCE_LOG_TAG = "TaskMockDataSource"

/**
 * 任务内存模拟数据源：提供增删改查能力，进程内单例存储。
 *
 * **日志**：仅 Debug 级操作埋点；业务异常直接抛出，不在本层打印 Error（与 [ArticleLocalDataSource] 规则一致）。
 *
 * 扩展入口：后续可引入 Room，将实现迁移至 `TaskLocalDataSource` 并在此类或工厂处切换数据源。
 */
class TaskMockDataSource {

    private val mutex = Mutex()
    private val taskStore = mutableMapOf<String, Task>()

    /**
     * 查询全部任务（按创建时间倒序）。
     */
    suspend fun getAll(): List<Task> = mutex.withLock {
        taskStore.values.sortedByDescending { it.createdAt }
    }.also { tasks ->
        logTaskDataDebug("内存读取完成 count=${tasks.size}")
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
        logTaskDataDebug("写入任务完成 id=${task.id}")
    }

    /**
     * 更新任务；若 id 不存在则抛出 [TaskFlowIllegalStateException]。
     */
    suspend fun update(task: Task) = mutex.withLock {
        if (!taskStore.containsKey(task.id)) {
            throw TaskFlowIllegalStateException("任务不存在，无法更新: ${task.id}")
        }
        taskStore[task.id] = task
        logTaskDataDebug("更新任务完成 id=${task.id}")
    }

    /**
     * 删除任务；若 id 不存在则抛出 [TaskFlowIllegalStateException]。
     */
    suspend fun delete(id: String) = mutex.withLock {
        if (taskStore.remove(id) == null) {
            throw TaskFlowIllegalStateException("任务不存在，无法删除: $id")
        }
        logTaskDataDebug("删除任务完成 id=$id")
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
        logTaskDataDebug("写入演示种子数据")
    }

    private fun logTaskDataDebug(message: String) {
        TaskFlowLogger.d(TASK_DATA_SOURCE_LOG_TAG) { message }
    }
}
