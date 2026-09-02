package com.example.zhttaskflow.feature.log.domain.usecase

import com.example.zhttaskflow.feature.log.domain.LogCategory
import com.example.zhttaskflow.feature.log.domain.LogExportScope
import com.example.zhttaskflow.feature.log.domain.LogQueryFilter
import com.example.zhttaskflow.feature.log.domain.repository.LogRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * [ExportLogsUseCase] 导出范围与仓库调用参数的单测。
 */
class ExportLogsUseCaseTest {

    private val logRepository = mockk<LogRepository>()
    private val useCase = ExportLogsUseCase(logRepository)

    @Test
    fun invoke_currentFilter_usesListFilter() = runTest {
        val listFilter = LogQueryFilter(logType = LogCategory.CRASH)
        val exportFile = tempJsonlFile()
        coEvery { logRepository.exportLogs(listFilter, 2_000) } returns exportFile
        val result = useCase(listFilter = listFilter, scope = LogExportScope.CURRENT_FILTER)
        assertEquals(exportFile, result)
        coVerify { logRepository.exportLogs(listFilter, 2_000) }
    }

    @Test
    fun invoke_allLogs_ignoresTypeOnListFilter() = runTest {
        val listFilter = LogQueryFilter(logType = LogCategory.PERFORMANCE)
        val allFilter = LogQueryFilter(logType = null)
        val exportFile = tempJsonlFile()
        coEvery { logRepository.exportLogs(allFilter, 2_000) } returns exportFile
        val result = useCase(listFilter = listFilter, scope = LogExportScope.ALL_LOGS)
        assertEquals(exportFile, result)
        coVerify { logRepository.exportLogs(allFilter, 2_000) }
        coVerify(exactly = 0) { logRepository.exportLogs(listFilter, any()) }
    }

    @Test
    fun invoke_customMaxEntries_passedToRepository() = runTest {
        val filter = LogQueryFilter()
        val exportFile = tempJsonlFile()
        coEvery { logRepository.exportLogs(filter, 128) } returns exportFile
        useCase(listFilter = filter, scope = LogExportScope.CURRENT_FILTER, maxEntries = 128)
        coVerify { logRepository.exportLogs(filter, 128) }
    }

    @Test
    fun invoke_returnsJsonlFileFromRepository() = runTest {
        val filter = LogQueryFilter()
        val exportFile = tempJsonlFile(
            """{"timestampEpochMs":1,"logType":"ANALYTICS","event":"open"}""",
        )
        coEvery { logRepository.exportLogs(filter, 2_000) } returns exportFile
        val result = useCase(listFilter = filter, scope = LogExportScope.ALL_LOGS)
        assertTrue(result.name.endsWith(".jsonl"))
        val lines = result.readLines()
        assertEquals(1, lines.size)
        assertTrue(lines.single().contains("\"event\":\"open\""))
    }

    private fun tempJsonlFile(vararg lines: String): File {
        return File.createTempFile("log_export", ".jsonl").apply {
            if (lines.isNotEmpty()) {
                writeText(lines.joinToString("\n") + "\n")
            }
        }
    }
}
