package com.example.zhttaskflow.core.persistence.room

import android.content.Context
import androidx.room.RoomDatabase

/**
 * Room 数据库打开兼容入口，委托 [TaskFlowRoomTemplate.openDao]。
 *
 * **调用方式**：业务 data 层优先直接使用 [TaskFlowRoomTemplate]；本对象保留 [openDao] 别名以兼容旧引用。
 *
 * **线程约束**：数据库操作须在 [TaskFlowRoomTemplate.runWithDao] / [runIo] 中执行，禁止主线程直接调用 DAO。
 */
object TaskFlowRoomDatabaseBuilder {

    /**
     * 打开 DAO，不向外暴露 [RoomDatabase] 实例。
     */
    fun <DB : RoomDatabase, D : BaseRoomDao> openDao(
        context: Context,
        config: TaskFlowRoomConfig,
        databaseClass: Class<DB>,
        daoProvider: (DB) -> D,
    ): D = TaskFlowRoomTemplate.openDao(context, config, databaseClass, daoProvider)
}
