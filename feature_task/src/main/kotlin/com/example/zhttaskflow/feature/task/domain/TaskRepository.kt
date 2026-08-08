package com.example.zhttaskflow.feature.task.domain

/**
 * 任务仓库抽象：定义任务增删改查能力契约，由 data 层实现并注入 domain / presentation。
 *
 * 遵循依赖倒置：领域层只依赖本接口，不依赖具体数据源（网络、数据库等）。
 */
interface TaskRepository {

    /**
     * 按 id 查询任务。
     *
     * @return 存在则返回 [Task]，不存在返回 null
     */
    suspend fun getTaskById(id: String): Task?

    /**
     * 查询全部任务列表。
     */
    suspend fun getAllTasks(): List<Task>

    /**
     * 新增任务。
     *
     * @param task 待持久化的领域实体
     */
    suspend fun addTask(task: Task)

    /**
     * 更新已有任务（通常按 [Task.id] 匹配）。
     *
     * @param task 更新后的领域实体
     */
    suspend fun updateTask(task: Task)

    /**
     * 按 id 删除任务。
     *
     * @param id 任务唯一标识
     */
    suspend fun deleteTask(id: String)
}
