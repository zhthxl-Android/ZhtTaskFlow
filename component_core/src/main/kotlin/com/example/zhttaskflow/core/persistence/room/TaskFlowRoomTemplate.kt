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
 * Room 统一门面：业务 data 层访问本地数据库的 **唯一推荐入口**。
 *
 * ## 防腐约定
 * - 所有 [Room.databaseBuilder] 等 Room **运行时 API** 仅在本类内部使用；
 * - 业务层仅通过 [openDao] 获取 DAO，通过 [runWithDao] / [runIo] 执行挂起操作；
 * - 禁止直接持有 [RoomDatabase]、禁止自行管理数据库生命周期。
 *
 * ## 线程与异常
 * - 所有数据库操作在 [Dispatchers.IO] 上执行，主线程安全；
 * - 非 [CancellationException] 异常统一记录 [TaskFlowLogger] 并包装为 [TaskFlowIllegalStateException]。
 *
 * ## 声明式注解
 * 业务模块仍可在 DAO / Entity 上使用 Room **编译期注解**（@Dao、@Entity、@Query 等），
 * 由各模块 KSP 生成实现；运行时能力统一经本门面访问。
 */
object TaskFlowRoomTemplate {

    private const val DEFAULT_LOG_TAG = "TaskFlowRoomTemplate"

    private val databaseCache = ConcurrentHashMap<String, RoomDatabase>()

    /**
     * 打开 DAO：内部创建或复用数据库实例，生命周期由本门面缓存管理。
     *
     * @param context 建议使用 [Context.getApplicationContext]
     * @param config 数据库名等配置
     * @param databaseClass `@Database` 注解的类
     * @param daoProvider 从数据库类提取 DAO（如 `{ it.articleDao() }`）
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
     * 在 IO 线程上执行与指定 DAO 相关的挂起数据库逻辑（带统一异常兜底）。
     *
     * @param dao 已通过 [openDao] 获取的 DAO
     * @param tag 日志 Tag
     * @param block 数据库挂起操作，参数为 [dao]
     */
    suspend fun <D : BaseRoomDao, T> runWithDao(
        dao: D,
        tag: String = DEFAULT_LOG_TAG,
        block: suspend (D) -> T,
    ): T = executeOnRoomIo(tag) {
        block(dao)
    }

    /**
     * 在 IO 线程上执行挂起数据库逻辑（DAO 已在 block 内闭包引用时常用）。
     *
     * @param tag 日志 Tag
     * @param block 数据库挂起操作
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

    /**
     * 获取数据库实例（仅供 [TaskFlowRoomDatabaseBuilder.build] 历史兼容，业务模块禁止调用）。
     */
    internal fun <T : RoomDatabase> openDatabaseInstance(
        context: Context,
        config: TaskFlowRoomConfig,
        databaseClass: Class<T>,
    ): T = obtainDatabase(context, config, databaseClass)
}
