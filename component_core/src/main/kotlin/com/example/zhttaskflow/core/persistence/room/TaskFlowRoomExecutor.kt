package com.example.zhttaskflow.core.persistence.room

/**
 * Room IO 扩展（委托 [TaskFlowRoomTemplate]）。
 *
 * 推荐在 data 层使用 [TaskFlowRoomTemplate.runWithDao] 或 [TaskFlowRoomTemplate.runIo]。
 */

/**
 * 在 core 封装的 IO 线程与异常兜底下执行 DAO 相关挂起逻辑。
 */
suspend fun <T> BaseRoomDao.runRoomIo(
    tag: String = "TaskFlowRoom",
    block: suspend () -> T,
): T = TaskFlowRoomTemplate.runIo(tag = tag, block = block)
