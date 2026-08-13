package com.example.zhttaskflow.core.persistence.room

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room 数据库统一构建入口。
 *
 * 安全说明：默认未启用 SQLCipher；敏感数据场景请在业务层接入加密或 EncryptedFile 扩展。
 */
object TaskFlowRoomDatabaseBuilder {

    /**
     * 创建 [RoomDatabase] 子类实例。
     *
     * @param context ApplicationContext
     * @param config 数据库名等配置
     * @param databaseClass `@Database` 注解的类
     */
    fun <T : RoomDatabase> build(
        context: Context,
        config: TaskFlowRoomConfig,
        databaseClass: Class<T>,
    ): T {
        return Room.databaseBuilder(
            context.applicationContext,
            databaseClass,
            config.databaseName,
        ).build()
    }
}
