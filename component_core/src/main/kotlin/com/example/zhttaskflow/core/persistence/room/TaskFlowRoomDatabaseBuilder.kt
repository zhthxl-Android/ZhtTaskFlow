package com.example.zhttaskflow.core.persistence.room

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room 构建入口：调用方传入已定义的 [RoomDatabase] 子类与配置。
 */
object TaskFlowRoomDatabaseBuilder {

    /**
     * 创建 Room 数据库实例。
     *
     * @param context ApplicationContext
     * @param databaseClass 业务定义的 Database 类型（须含 Entity，本框架不包含业务表）
     */
    fun <T : RoomDatabase> build(
        context: Context,
        databaseClass: Class<T>,
        config: TaskFlowRoomConfig,
    ): T {
        val builder = Room.databaseBuilder(
            context.applicationContext,
            databaseClass,
            config.databaseName,
        )
        return builder.build()
    }
}
