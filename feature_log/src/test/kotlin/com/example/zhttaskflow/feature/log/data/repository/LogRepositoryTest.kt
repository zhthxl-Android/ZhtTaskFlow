package com.example.zhttaskflow.feature.log.data.repository

import android.content.Context
import com.example.zhttaskflow.core.observability.TaskFlowLocalLogStore
import com.example.zhttaskflow.feature.log.domain.LogCategory
import com.example.zhttaskflow.feature.log.domain.LogEntry
import com.example.zhttaskflow.feature.log.domain.LogQueryFilter
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.Runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * [LogRepositoryImpl] 与本地日志存储委托行为的单元测试（Mock Store 单例，无真机依赖）。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LogRepositoryTest {

    private val appContext = mockk<Context>(relaxed = true)
    private val repository = LogRepositoryImpl(appContext)

    @Before
    fun setUp() {
        mockkObject(TaskFlowLocalLogStore)
    }

    @After
    fun tearDown() {
        unmockkObject(TaskFlowLocalLogStore)
    }

    @Test
    fun queryPaged_mapsPageAndEntries() = runTest {
        val storeRecord = sampleStoreRecord()
        val filterSlot = slot<TaskFlowLocalLogStore.PagedQueryFilter>()
        every { TaskFlowLocalLogStore.queryPaged(capture(filterSlot)) } returns
            TaskFlowLocalLogStore.PagedQueryResult(
                records = listOf(storeRecord),
                hasMore = true,
            )
        val page = repository.queryPaged(
            filter = LogQueryFilter(logType = LogCategory.ANALYTICS),
            page = 2,
            pageSize = 25,
        )
        assertEquals(1, page.entries.size)
        assertTrue(page.hasMore)
        assertEquals(LogCategory.ANALYTICS, page.entries.single().category)
        assertEquals(2, filterSlot.captured.page)
        assertEquals(25, filterSlot.captured.pageSize)
        assertEquals(TaskFlowLocalLogStore.LogType.ANALYTICS, filterSlot.captured.logType)
    }

    @Test
    fun queryPaged_appliesTypeFilter() = runTest {
        val filterSlot = slot<TaskFlowLocalLogStore.PagedQueryFilter>()
        every { TaskFlowLocalLogStore.queryPaged(capture(filterSlot)) } returns
            TaskFlowLocalLogStore.PagedQueryResult(emptyList(), hasMore = false)
        repository.queryPaged(LogQueryFilter(logType = LogCategory.CRASH), page = 0, pageSize = 50)
        assertEquals(TaskFlowLocalLogStore.LogType.CRASH, filterSlot.captured.logType)
        repository.queryPaged(LogQueryFilter(logType = null), page = 0, pageSize = 50)
        assertEquals(null, filterSlot.captured.logType)
    }

    @Test
    fun clearAllLogs_delegatesToStore() = runTest {
        every { TaskFlowLocalLogStore.clearAllLogs() } just Runs
        repository.clearAllLogs()
        verify(exactly = 1) { TaskFlowLocalLogStore.clearAllLogs() }
    }

    @Test
    fun exportLogs_passesMappedFilterAndReturnsFile() = runTest {
        val filterSlot = slot<TaskFlowLocalLogStore.QueryFilter>()
        val exportFile = File.createTempFile("taskflow_export", ".jsonl")
        every {
            TaskFlowLocalLogStore.exportRecentLogs(appContext, capture(filterSlot), 500)
        } returns exportFile
        val result = repository.exportLogs(LogQueryFilter(logType = LogCategory.PERFORMANCE), maxEntries = 500)
        assertEquals(exportFile, result)
        assertEquals(TaskFlowLocalLogStore.LogType.PERFORMANCE, filterSlot.captured.logType)
        assertEquals(500, filterSlot.captured.maxEntries)
    }

    @Test
    fun encodeLogDetail_delegatesToStoreEncoder() = runTest {
        val entry = LogEntry(
            id = "id",
            timestampEpochMs = 100L,
            category = LogCategory.ANALYTICS,
            typeLabel = "埋点",
            pageId = "p",
            actionId = "a",
            event = "ev",
            params = emptyMap(),
        )
        every { TaskFlowLocalLogStore.encodeRecord(any()) } returns """{"event":"ev"}"""
        val encoded = repository.encodeLogDetail(entry)
        assertEquals("""{"event":"ev"}""", encoded)
    }

    private fun sampleStoreRecord(): TaskFlowLocalLogStore.LogRecord {
        return TaskFlowLocalLogStore.LogRecord(
            timestampEpochMs = 1L,
            logType = TaskFlowLocalLogStore.LogType.ANALYTICS,
            pageId = "Home",
            actionId = "click",
            event = "tap",
            params = emptyMap(),
        )
    }
}
