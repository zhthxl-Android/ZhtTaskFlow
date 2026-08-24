package com.example.zhttaskflow.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StringExtensionsTest {

    @Test
    fun orEmpty_null_returns_empty() {
        val value: String? = null
        assertEquals("", value.orEmpty())
    }

    @Test
    fun orEmpty_preserves_blank() {
        assertEquals("  ", "  ".orEmpty())
    }

    @Test
    fun blankToEmpty_null_and_blank() {
        assertEquals("", null.blankToEmpty())
        assertEquals("", "".blankToEmpty())
        assertEquals("", "   ".blankToEmpty())
    }

    @Test
    fun blankToEmpty_trimmed_content() {
        assertEquals("a", "  a  ".blankToEmpty())
    }

    @Test
    fun nullIfBlank_maps_blank_to_null() {
        assertNull(null.nullIfBlank())
        assertNull("".nullIfBlank())
        assertNull("  ".nullIfBlank())
        assertEquals("ok", "  ok  ".nullIfBlank())
    }

    @Test
    fun isNotNullOrBlank_and_isNotNullOrEmpty() {
        assertFalse(null.isNotNullOrBlank())
        assertFalse("  ".isNotNullOrBlank())
        assertTrue("x".isNotNullOrBlank())

        assertFalse(null.isNotNullOrEmpty())
        assertFalse("".isNotNullOrEmpty())
        assertTrue("  ".isNotNullOrEmpty())
    }
}
