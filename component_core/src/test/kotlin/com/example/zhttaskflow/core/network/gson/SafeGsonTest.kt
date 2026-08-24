package com.example.zhttaskflow.core.network.gson

import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeGsonTest {

    private val gson = buildSafeGson()

    @Test
    fun `null root list json becomes empty list`() {
        val type = object : TypeToken<List<String>>() {}.type
        val result = gson.fromJson<List<String>>("null", type)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `null list field becomes empty list`() {
        val dto = gson.fromJson("""{"items":null}""", ListHolder::class.java)
        assertTrue(dto.items.isEmpty())
    }

    @Test
    fun `null string field becomes empty string`() {
        val dto = gson.fromJson("""{"name":null}""", StringHolder::class.java)
        assertEquals("", dto.name)
    }

    @Test
    fun `nullable entity field stays null`() {
        val dto = gson.fromJson("""{"child":null}""", EntityHolder::class.java)
        assertNull(dto.child)
    }

    private data class ListHolder(val items: List<String>)

    private data class StringHolder(val name: String)

    private data class EntityHolder(val child: Child?)

    private data class Child(val id: String)
}
