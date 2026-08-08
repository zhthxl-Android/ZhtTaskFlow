package com.example.zhttaskflow.core.persistence.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore Preferences 工厂：按文件名缓存单例，调用方手动持有引用。
 */
object TaskFlowPreferencesDataStoreFactory {

    private val cache = mutableMapOf<String, DataStore<Preferences>>()

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

/** 读取 String */
fun DataStore<Preferences>.readString(key: String, default: String = ""): Flow<String> =
    data.map { prefs -> prefs[stringPreferencesKey(key)] ?: default }

/** 写入 String */
suspend fun DataStore<Preferences>.writeString(key: String, value: String) {
    edit { prefs -> prefs[stringPreferencesKey(key)] = value }
}

/** 读取 Boolean */
fun DataStore<Preferences>.readBoolean(key: String, default: Boolean = false): Flow<Boolean> =
    data.map { prefs -> prefs[booleanPreferencesKey(key)] ?: default }

/** 写入 Boolean */
suspend fun DataStore<Preferences>.writeBoolean(key: String, value: Boolean) {
    edit { prefs -> prefs[booleanPreferencesKey(key)] = value }
}

/** 读取 Int */
fun DataStore<Preferences>.readInt(key: String, default: Int = 0): Flow<Int> =
    data.map { prefs -> prefs[intPreferencesKey(key)] ?: default }

/** 写入 Int */
suspend fun DataStore<Preferences>.writeInt(key: String, value: Int) {
    edit { prefs -> prefs[intPreferencesKey(key)] = value }
}
