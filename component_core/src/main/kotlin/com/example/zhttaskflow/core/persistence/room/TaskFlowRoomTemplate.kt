package com.example.zhttaskflow.core.persistence.room

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.zhttaskflow.base.foundation.TaskFlowIllegalStateException
import com.example.zhttaskflow.base.foundation.TaskFlowLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Room 统一门面：封装数据库实例创建、DAO 获取与 IO 执行，业务 data 层访问本地库的唯一推荐入口。
 *
 * **调用方式**：
 * - 获取 DAO：[openDao]
 * - 执行读写：[runWithDao]（推荐）或 [runIo]（block 内已持有 DAO 时）
 * - 业务模块仅使用 Room **编译期注解**（@Entity / @Dao），禁止调用 [Room.databaseBuilder]
 *
 * **线程约束**：所有 [runWithDao] / [runIo] 均在 [Dispatchers.IO] 执行，主线程安全；
 * 非 [CancellationException] 统一记录日志并包装为 [TaskFlowIllegalStateException]。
 */
object TaskFlowRoomTemplate {

    private const val DEFAULT_LOG_TAG = "TaskFlowRoomTemplate"

    private val databaseCache = ConcurrentHashMap<String, RoomDatabase>()

    /**
     * 打开 DAO：内部创建或复用数据库实例，生命周期由本门面缓存管理。
     */
    fun <DB : RoomDatabase, D : BaseRoomDao> openDao(
        context: Context,
        config: TaskFlowRoomConfig,
        databaseClass: Class<DB>,
        daoProvider: (DB) -> D,
    ): D {
        val database = obtainDatabase(context, config, databaseClass)
        return daoProvider(database)
    }

    /**
     * 在 IO 线程执行与指定 DAO 相关的挂起逻辑（带统一异常兜底）。
     */
    suspend fun <D : BaseRoomDao, T> runWithDao(
        dao: D,
        tag: String = DEFAULT_LOG_TAG,
        block: suspend (D) -> T,
    ): T = executeOnRoomIo(tag) {
        block(dao)
    }

    /**
     * 在 IO 线程执行挂起数据库逻辑（block 内已闭包引用 DAO 时使用）。
     */
    suspend fun <T> runIo(
        tag: String = DEFAULT_LOG_TAG,
        block: suspend () -> T,
    ): T = executeOnRoomIo(tag, block)

    @Suppress("UNCHECKED_CAST")
    private fun <T : RoomDatabase> obtainDatabase(
        context: Context,
        config: TaskFlowRoomConfig,
        databaseClass: Class<T>,
    ): T {
        return databaseCache.getOrPut(config.databaseName) {
            Room.databaseBuilder(
                context.applicationContext,
                databaseClass,
                config.databaseName,
            ).build()
        } as T
    }

    private suspend fun <T> executeOnRoomIo(
        tag: String,
        block: suspend () -> T,
    ): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            TaskFlowLogger.e(tag, throwable.message ?: "Room 操作失败", throwable)
            throw TaskFlowIllegalStateException(
                message = "本地数据库操作失败",
                cause = throwable,
            )
        }
    }
}
