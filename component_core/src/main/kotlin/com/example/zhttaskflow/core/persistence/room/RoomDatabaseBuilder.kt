package com.example.zhttaskflow.core.persistence.room

import android.content.Context
import androidx.room.RoomDatabase

/**
 * Room 数据库打开兼容入口，委托 [RoomTemplate.openDao]。
 *
 * **调用方式**：业务 data 层优先直接使用 [RoomTemplate]；本对象保留 [openDao] 别名以兼容旧引用。
 *
 * **线程约束**：数据库操作须在 [RoomTemplate.runWithDao] / [runIo] 中执行，禁止主线程直接调用 DAO。
 */
object RoomDatabaseBuilder {

    /**
     * 打开 DAO，不向外暴露 [RoomDatabase] 实例。
     */
    fun <DB : RoomDatabase, D : BaseRoomDao> openDao(
        context: Context,
        config: RoomConfig,
        databaseClass: Class<DB>,
        daoProvider: (DB) -> D,
    ): D = RoomTemplate.openDao(context, config, databaseClass, daoProvider)
}
