package com.example.zhttaskflow.core.persistence.room

import android.content.Context
import androidx.room.RoomDatabase

/**
 * Room 数据库构建兼容入口（委托 [TaskFlowRoomTemplate]）。
 *
 * 新代码请直接使用 [TaskFlowRoomTemplate]；本对象保留以兼容既有 [openDao] 调用，行为与门面一致。
 */
object TaskFlowRoomDatabaseBuilder {

    /**
     * 通过门面打开 DAO，不向外暴露 [RoomDatabase] 实例。
     */
    fun <DB : RoomDatabase, D : BaseRoomDao> openDao(
        context: Context,
        config: TaskFlowRoomConfig,
        databaseClass: Class<DB>,
        daoProvider: (DB) -> D,
    ): D = TaskFlowRoomTemplate.openDao(context, config, databaseClass, daoProvider)

    /**
     * 获取数据库实例（仅供历史兼容；业务模块请勿调用，生命周期由 [TaskFlowRoomTemplate] 管理）。
     */
    fun <T : RoomDatabase> build(
        context: Context,
        config: TaskFlowRoomConfig,
        databaseClass: Class<T>,
    ): T = TaskFlowRoomTemplate.openDatabaseInstance(context, config, databaseClass)
}
