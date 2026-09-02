package com.example.zhttaskflow.feature.log.presentation

import android.util.Log
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.feature.log.domain.LogCategory
import com.example.zhttaskflow.feature.log.domain.LogExportScope
import com.example.zhttaskflow.feature.log.domain.LogEntry
import com.example.zhttaskflow.feature.log.domain.LogPage
import com.example.zhttaskflow.feature.log.domain.LogQueryFilter
import com.example.zhttaskflow.feature.log.domain.repository.LogRepository
import com.example.zhttaskflow.feature.log.domain.usecase.ClearLogsUseCase
import com.example.zhttaskflow.feature.log.domain.usecase.ExportLogsUseCase
import com.example.zhttaskflow.feature.log.domain.usecase.LogDisplayUseCase
import com.example.zhttaskflow.feature.log.domain.usecase.QueryLogsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModelTest {

    private val logRepository = mockk<LogRepository>(relaxed = true)
    private val queryLogsUseCase = QueryLogsUseCase(logRepository)
    private val exportLogsUseCase = ExportLogsUseCase(logRepository)
    private val clearLogsUseCase = ClearLogsUseCase(logRepository)
    private val logDisplayUseCase = LogDisplayUseCase(logRepository)

    @Before
    fun setUp() {
        mockAndroidLog()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
    }

    @Test
    fun init_noLogs_emitsEmpty() = viewModelTest {
        coEvery {
            logRepository.queryPaged(any(), any(), any())
        } returns LogPage(entries = emptyList(), hasMore = false)
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(BaseUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun init_withLogs_emitsSuccess() = viewModelTest {
        val entry = sampleEntry()
        coEvery {
            logRepository.queryPaged(LogQueryFilter(), 0, 50)
        } returns LogPage(entries = listOf(entry), hasMore = false)
        every { logRepository.formatTimestamp(entry.timestampEpochMs) } returns "ts"
        val viewModel = createViewModel()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is BaseUiState.Success)
        assertEquals(1, (state as BaseUiState.Success).data.entries.size)
    }

    @Test
    fun export_currentFilter_usesListFilter() = viewModelTest {
        coEvery {
            logRepository.queryPaged(any(), any(), any())
        } returns LogPage(entries = emptyList(), hasMore = false)
        val exportFile = File.createTempFile("log_export", ".jsonl")
        coEvery {
            logRepository.exportLogs(LogQueryFilter(logType = LogCategory.CRASH), 2_000)
        } returns exportFile
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogUiEvent.FilterSelected(LogTypeFilter.CRASH))
        advanceUntilIdle()
        viewModel.onEvent(LogUiEvent.ExportConfirmed(LogExportScope.CURRENT_FILTER))
        advanceUntilIdle()
        coVerify {
            logRepository.exportLogs(LogQueryFilter(logType = LogCategory.CRASH), 2_000)
        }
    }

    @Test
    fun export_all_ignoresTypeFilter() = viewModelTest {
        coEvery {
            logRepository.queryPaged(any(), any(), any())
        } returns LogPage(entries = emptyList(), hasMore = false)
        val exportFile = File.createTempFile("log_export", ".jsonl")
        coEvery {
            logRepository.exportLogs(LogQueryFilter(logType = null), 2_000)
        } returns exportFile
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(LogUiEvent.FilterSelected(LogTypeFilter.CRASH))
        advanceUntilIdle()
        viewModel.onEvent(LogUiEvent.ExportConfirmed(LogExportScope.ALL_LOGS))
        advanceUntilIdle()
        coVerify {
            logRepository.exportLogs(LogQueryFilter(logType = null), 2_000)
        }
    }

    private fun viewModelTest(block: suspend TestScope.() -> Unit): Unit = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        every { logRepository.formatTimestamp(any()) } returns "2026-01-01"
        every { logRepository.encodeLogDetail(any()) } returns "{}"
        block()
    }

    private fun createViewModel(): LogViewModel {
        val appContext = mockk<android.content.Context>(relaxed = true)
        return LogViewModel(
            appContext = appContext,
            queryLogsUseCase = queryLogsUseCase,
            exportLogsUseCase = exportLogsUseCase,
            clearLogsUseCase = clearLogsUseCase,
            logDisplayUseCase = logDisplayUseCase,
        )
    }

    private fun sampleEntry(): LogEntry {
        return LogEntry(
            id = "id-1",
            timestampEpochMs = 1L,
            category = LogCategory.ANALYTICS,
            typeLabel = "埋点",
            pageId = "p",
            actionId = "a",
            event = "ev",
            params = emptyMap(),
        )
    }

    private fun mockAndroidLog() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.i(any(), any()) } returns 0
    }
}
