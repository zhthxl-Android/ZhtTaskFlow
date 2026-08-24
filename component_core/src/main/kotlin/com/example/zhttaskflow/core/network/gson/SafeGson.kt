package com.example.zhttaskflow.core.network.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * 构建带空安全兜底的全局 [Gson] 实例（Retrofit 与 core 网络层统一入口）。
 *
 * - [List] / [Set]：JSON `null` 或反序列化结果为 `null` 时转为空集合；
 * - [String]：JSON `null` 或结果为 `null` 时转为 `""`；
 * - 其它类型（含自定义 DTO、可空引用类型字段）保持 Gson 默认行为，实体级 `null` 由 Mapper 处理。
 */
fun buildSafeGson(): Gson {
    return GsonBuilder()
        .registerTypeAdapterFactory(SafeGsonAdapterFactory())
        .create()
}

/**
 * Gson 反序列化空值兜底工厂：仅作用于 [String]、[List]、[Set]，不扩大兜底范围。
 */
class SafeGsonAdapterFactory : TypeAdapterFactory {

    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType
        when {
            rawType == String::class.java -> {
                val delegate = gson.getDelegateAdapter(this, TypeToken.get(String::class.java))
                @Suppress("UNCHECKED_CAST")
                return SafeStringTypeAdapter(delegate) as TypeAdapter<T>
            }
            List::class.java.isAssignableFrom(rawType) -> {
                val delegate = gson.getDelegateAdapter(this, type)
                @Suppress("UNCHECKED_CAST")
                return SafeListTypeAdapter(delegate) as TypeAdapter<T>
            }
            Set::class.java.isAssignableFrom(rawType) -> {
                val delegate = gson.getDelegateAdapter(this, type)
                @Suppress("UNCHECKED_CAST")
                return SafeSetTypeAdapter(delegate) as TypeAdapter<T>
            }
            else -> return null
        }
    }

    private class SafeStringTypeAdapter(
        private val delegate: TypeAdapter<String>,
    ) : TypeAdapter<String>() {
        override fun write(out: JsonWriter, value: String?) {
            delegate.write(out, value)
        }

        override fun read(reader: JsonReader): String {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                return ""
            }
            return delegate.read(reader) ?: ""
        }
    }

    private class SafeListTypeAdapter<T>(
        private val delegate: TypeAdapter<T>,
    ) : TypeAdapter<T>() {
        override fun write(out: JsonWriter, value: T?) {
            delegate.write(out, value)
        }

        override fun read(reader: JsonReader): T {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                @Suppress("UNCHECKED_CAST")
                return emptyList<Any>() as T
            }
            val value = delegate.read(reader)
            if (value == null) {
                @Suppress("UNCHECKED_CAST")
                return emptyList<Any>() as T
            }
            return value
        }
    }

    private class SafeSetTypeAdapter<T>(
        private val delegate: TypeAdapter<T>,
    ) : TypeAdapter<T>() {
        override fun write(out: JsonWriter, value: T?) {
            delegate.write(out, value)
        }

        override fun read(reader: JsonReader): T {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                @Suppress("UNCHECKED_CAST")
                return emptySet<Any>() as T
            }
            val value = delegate.read(reader)
            if (value == null) {
                @Suppress("UNCHECKED_CAST")
                return emptySet<Any>() as T
            }
            return value
        }
    }
}
