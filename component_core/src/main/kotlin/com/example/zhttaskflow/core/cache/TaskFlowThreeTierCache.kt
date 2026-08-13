package com.example.zhttaskflow.core.cache

import com.example.zhttaskflow.base.foundation.TaskFlowLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val CACHE_LOG_TAG = "TaskFlowThreeTierCache"

/**
 * 三级缓存策略：内存 → 本地存储（Room 等）→ 远程（网络）。
 *
 * 一致性规则：远程成功后回写本地与内存；远程失败时若已有本地/内存数据则保持降级，不抛错中断 Flow。
 * 所有本地/远程读写均在 [Dispatchers.IO] 执行。
 */
class TaskFlowThreeTierCache<Key, Value>(
    private val memoryCache: MutableMap<Key, Value> = mutableMapOf(), // 内存缓存，使用可变Map实现
    private val readLocal: suspend (Key) -> Value?, // 本地数据读取函数，参数为Key，返回值为可空的Value
    private val readRemote: suspend (Key) -> Value, // 远程数据读取函数，参数为Key，返回值为Value
    private val writeLocal: suspend (Key, Value) -> Unit, // 本地数据写入函数，参数为Key和Value
) {

    private val mutex = Mutex() // 互斥锁，用于保证内存缓存的线程安全

    /**
     * 按三级顺序发射数据：内存 → 本地 → 远程；远程失败时保留已发射的本地/内存数据。
     * @param key 数据的键
     * @return Flow<Value> 数据流，按顺序发射各级缓存中的数据
     */
    fun observe(key: Key): Flow<Value> = flow {
        // 首先尝试从内存缓存获取数据
        var fallback: Value? = mutex.withLock { memoryCache[key] }
        if (fallback != null) {
            emit(fallback) // 如果内存中有数据，立即发射并继续检查下一级
        }
        // 然后尝试从本地存储获取数据
        val local = withContext(Dispatchers.IO) {
            runCatching { readLocal(key) }.getOrNull() // 捕获可能的异常，返回null表示读取失败
        }
        if (local != null) {
            fallback = local // 更新fallback为本地数据
            mutex.withLock { memoryCache[key] = local } // 将本地数据存入内存缓存
            emit(local) // 发射本地数据
        }
        // 最后尝试从远程获取数据
        try {
            val remote = withContext(Dispatchers.IO) { readRemote(key) } // 从远程获取数据
            withContext(Dispatchers.IO) { writeLocal(key, remote) } // 将远程数据写入本地存储
            mutex.withLock { memoryCache[key] = remote } // 将远程数据存入内存缓存
            emit(remote) // 发射远程数据
        } catch (cancellation: CancellationException) {
            throw cancellation // 如果是取消异常，直接抛出
        } catch (throwable: Throwable) {
            // 处理其他异常
            TaskFlowLogger.w(
                CACHE_LOG_TAG,
                "远程层加载失败，已降级为本地/内存数据（若有）",
                throwable,
            )
            if (fallback == null) {
                throw throwable // 如果没有降级数据，则抛出异常
            }
        }
    }

    /**
     * 强制走远程并刷新各级缓存。
     * @param key 要刷新的数据的键
     * @return Value 远程获取的最新数据
     */
    suspend fun refresh(key: Key): Value = withContext(Dispatchers.IO) {
        val remote = readRemote(key) // 从远程获取数据
        writeLocal(key, remote) // 将数据写入本地存储
        mutex.withLock { memoryCache[key] = remote } // 将数据存入内存缓存
        remote // 返回最新数据
    }

    /** 清空内存层（线程安全） */
    suspend fun clearMemory() {
        mutex.withLock { memoryCache.clear() }
    }
}
