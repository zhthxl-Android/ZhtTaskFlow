package com.example.zhttaskflow.core.cache

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [TaskFlowThreeTierCache] 三级缓存时序与降级策略单元测试。
 */
class TaskFlowThreeTierCacheTest {

    private val key = "article-page-1"

    @Test
    fun memoryCacheHit_consecutiveCallsDoNotTriggerLocalOrRemote() = runTest {
        val memory = mutableMapOf(key to "memory-value")
        val readLocal = mockk<suspend (String) -> String?>()
        val readRemote = mockk<suspend (String) -> String>()
        val writeLocal = mockk<suspend (String, String) -> Unit>()

        val cache = TaskFlowThreeTierCache(
            memoryCache = memory,
            readLocal = readLocal,
            readRemote = readRemote,
            writeLocal = writeLocal,
        )

        assertEquals("memory-value", cache.observe(key).first())
        assertEquals("memory-value", cache.observe(key).first())

        coVerify(exactly = 0) { readLocal(any()) }
        coVerify(exactly = 0) { readRemote(any()) }
        coVerify(exactly = 0) { writeLocal(any(), any()) }
    }

    @Test
    fun localCacheHit_readsLocalAndWritesBackToMemory() = runTest {
        val readLocal = mockk<suspend (String) -> String?>()
        val readRemote = mockk<suspend (String) -> String>()
        val writeLocal = mockk<suspend (String, String) -> Unit>()

        coEvery { readLocal(key) } returns "local-value"
        coEvery { readRemote(key) } returns "remote-value"
        coEvery { writeLocal(key, "remote-value") } just Runs

        val cache = TaskFlowThreeTierCache(
            readLocal = readLocal,
            readRemote = readRemote,
            writeLocal = writeLocal,
        )

        val emissions = cache.observe(key).toList()

        assertEquals(listOf("local-value", "remote-value"), emissions)
        coVerify(exactly = 1) { readLocal(key) }
        coVerify(exactly = 1) { readRemote(key) }

        assertEquals("remote-value", cache.observe(key).first())
        coVerify(exactly = 1) { readLocal(key) }
        coVerify(exactly = 1) { readRemote(key) }
    }

    @Test
    fun networkFallback_whenMemoryAndLocalEmpty_triggersRemoteRead() = runTest {
        val readLocal = mockk<suspend (String) -> String?>()
        val readRemote = mockk<suspend (String) -> String>()
        val writeLocal = mockk<suspend (String, String) -> Unit>()

        coEvery { readLocal(key) } returns null
        coEvery { readRemote(key) } returns "remote-only"
        coEvery { writeLocal(key, "remote-only") } just Runs

        val cache = TaskFlowThreeTierCache(
            readLocal = readLocal,
            readRemote = readRemote,
            writeLocal = writeLocal,
        )

        val emissions = cache.observe(key).toList()

        assertEquals(listOf("remote-only"), emissions)
        coVerify(exactly = 1) { readLocal(key) }
        coVerify(exactly = 1) { readRemote(key) }
    }

    @Test
    fun networkSuccess_writesLocalAfterRemote() = runTest {
        val readLocal = mockk<suspend (String) -> String?>()
        val readRemote = mockk<suspend (String) -> String>()
        val writeLocal = mockk<suspend (String, String) -> Unit>()

        coEvery { readLocal(key) } returns null
        coEvery { readRemote(key) } returns "remote-payload"
        coEvery { writeLocal(key, "remote-payload") } just Runs

        val cache = TaskFlowThreeTierCache(
            readLocal = readLocal,
            readRemote = readRemote,
            writeLocal = writeLocal,
        )

        cache.observe(key).toList()

        coVerify(exactly = 1) { writeLocal(key, "remote-payload") }
    }

    @Test
    fun localReadFailure_degradesToRemoteWithoutBlocking() = runTest {
        val readLocal = mockk<suspend (String) -> String?>()
        val readRemote = mockk<suspend (String) -> String>()
        val writeLocal = mockk<suspend (String, String) -> Unit>()

        coEvery { readLocal(key) } throws IllegalStateException("local io error")
        coEvery { readRemote(key) } returns "remote-after-local-fail"
        coEvery { writeLocal(key, "remote-after-local-fail") } just Runs

        val cache = TaskFlowThreeTierCache(
            readLocal = readLocal,
            readRemote = readRemote,
            writeLocal = writeLocal,
        )

        val emissions = cache.observe(key).toList()

        assertEquals(listOf("remote-after-local-fail"), emissions)
        coVerify(exactly = 1) { readLocal(key) }
        coVerify(exactly = 1) { readRemote(key) }
        coVerify(exactly = 1) { writeLocal(key, "remote-after-local-fail") }
    }
}
