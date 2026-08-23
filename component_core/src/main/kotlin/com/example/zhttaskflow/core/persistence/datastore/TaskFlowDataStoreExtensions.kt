package com.example.zhttaskflow.core.persistence.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException as DataStoreIOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.zhttaskflow.base.foundation.TaskFlowIllegalStateException
import com.example.zhttaskflow.base.foundation.TaskFlowLogger
import com.example.zhttaskflow.core.network.TaskFlowNetworkDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private const val DATA_STORE_LOG_TAG = "TaskFlowDataStore"

/**
 * DataStore Preferences 单例工厂，按文件名缓存实例（线程安全）。
 */
object TaskFlowPreferencesDataStoreFactory {

    private val cache = ConcurrentHashMap<String, DataStore<Preferences>>()

    /**
     * 获取或创建指定文件名的 Preferences DataStore。
     *
     * @param context 建议使用 ApplicationContext
     * @param fileName 文件名（不含路径）
     */
    fun create(context: Context, fileName: String): DataStore<Preferences> {
        return cache.getOrPut(fileName) {
            PreferenceDataStoreFactory.create(
                produceFile = {
                    context.applicationContext.preferencesDataStoreFile(fileName)
                },
            )
        }
    }
}

/** 读取 String 偏好（Flow 主线程安全） */
fun DataStore<Preferences>.readString(key: String, default: String = ""): Flow<String> =
    data.map { prefs -> prefs[stringPreferencesKey(key)] ?: default }

/** 写入 String 偏好（IO 线程，失败记录日志并抛出 [TaskFlowIllegalStateException]） */
suspend fun DataStore<Preferences>.writeString(key: String, value: String) {
    runDataStoreIo("writeString") {
        edit { prefs -> prefs[stringPreferencesKey(key)] = value }
    }
}

/** 读取 Int 偏好 */
fun DataStore<Preferences>.readInt(key: String, default: Int = 0): Flow<Int> =
    data.map { prefs -> prefs[intPreferencesKey(key)] ?: default }

/** 写入 Int 偏好 */
suspend fun DataStore<Preferences>.writeInt(key: String, value: Int) {
    runDataStoreIo("writeInt") {
        edit { prefs -> prefs[intPreferencesKey(key)] = value }
    }
}

/** 读取 Boolean 偏好 */
fun DataStore<Preferences>.readBoolean(key: String, default: Boolean = false): Flow<Boolean> =
    data.map { prefs -> prefs[booleanPreferencesKey(key)] ?: default }

/** 写入 Boolean 偏好 */
suspend fun DataStore<Preferences>.writeBoolean(key: String, value: Boolean) {
    runDataStoreIo("writeBoolean") {
        edit { prefs -> prefs[booleanPreferencesKey(key)] = value }
    }
}

private suspend fun DataStore<Preferences>.runDataStoreIo(
    operation: String,
    block: suspend () -> Unit,
) {
    withContext(Dispatchers.IO) {
        try {
            block()
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (io: DataStoreIOException) {
            logDataStoreDebug("DataStore IO 失败: $operation", io)
            throw TaskFlowIllegalStateException(message = "本地偏好存储失败", cause = io)
        } catch (throwable: Throwable) {
            logDataStoreDebug("DataStore 操作失败: $operation", throwable)
            throw TaskFlowIllegalStateException(message = "本地偏好存储失败", cause = throwable)
        }
    }
}

private fun logDataStoreDebug(summary: String, throwable: Throwable) {
    if (!TaskFlowNetworkDiagnostics.isDebuggable) {
        return
    }
    TaskFlowLogger.d(
        DATA_STORE_LOG_TAG,
        "$summary\n${Log.getStackTraceString(throwable)}",
    )
}
