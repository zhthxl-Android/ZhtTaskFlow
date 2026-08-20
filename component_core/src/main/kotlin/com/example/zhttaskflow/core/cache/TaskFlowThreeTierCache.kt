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
 * 三级缓存模板：内存 → 本地（Room 等）→ 远程（网络），用于「先快后全」的数据加载场景。
 *
 * **执行时序（[observe]）**：
 * 1. 读取内存层并立即发射（若有）；
 * 2. 在 IO 线程读本地层，命中则回写内存并发射；
 * 3. 在 IO 线程拉远程，成功后写本地与内存并发射最新值；
 * 4. 远程失败时：若已有内存/本地数据则 **降级保留** 已发射数据；若三层皆无数据则抛出异常。
 *
 * **适用场景**：
 * - 列表/详情等可离线降级的读模型（配合 Repository 暴露 Flow）；
 * - 需要强制拉新时使用 [refresh]（不走降级，远程失败向上抛）。
 *
 * **调用方式**：构造时注入 `readLocal` / `readRemote` / `writeLocal`；订阅 [observe] 或调用 [refresh] / [clearMemory]。
 *
 * **线程约束**：本地与远程读写均在 [Dispatchers.IO]；内存层通过 [Mutex] 保证线程安全。
 */
class TaskFlowThreeTierCache<Key, Value>(
    private val memoryCache: MutableMap<Key, Value> = mutableMapOf(),
    private val readLocal: suspend (Key) -> Value?,
    private val readRemote: suspend (Key) -> Value,
    private val writeLocal: suspend (Key, Value) -> Unit,
) {

    private val mutex = Mutex()

    /**
     * 按三级顺序发射数据：内存 → 本地 → 远程；远程失败时保留已发射的本地/内存数据。
     */
    fun observe(key: Key): Flow<Value> = flow {
        var fallback: Value? = mutex.withLock { memoryCache[key] }
        if (fallback != null) {
            emit(fallback)
        }
        val local = withContext(Dispatchers.IO) {
            runCatching { readLocal(key) }.getOrNull()
        }
        if (local != null) {
            fallback = local
            mutex.withLock { memoryCache[key] = local }
            emit(local)
        }
        try {
            val remote = withContext(Dispatchers.IO) { readRemote(key) }
            withContext(Dispatchers.IO) { writeLocal(key, remote) }
            mutex.withLock { memoryCache[key] = remote }
            emit(remote)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            TaskFlowLogger.w(
                CACHE_LOG_TAG,
                "远程层加载失败，已降级为本地/内存数据（若有）",
                throwable,
            )
            if (fallback == null) {
                throw throwable
            }
        }
    }

    /**
     * 强制走远程并刷新各级缓存。
     */
    suspend fun refresh(key: Key): Value = withContext(Dispatchers.IO) {
        val remote = readRemote(key)
        writeLocal(key, remote)
        mutex.withLock { memoryCache[key] = remote }
        remote
    }

    /** 清空内存层（线程安全）。 */
    suspend fun clearMemory() {
        mutex.withLock { memoryCache.clear() }
    }
}
