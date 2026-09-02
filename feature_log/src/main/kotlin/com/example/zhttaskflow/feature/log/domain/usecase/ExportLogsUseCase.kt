package com.example.zhttaskflow.feature.log.domain.usecase

import com.example.zhttaskflow.feature.log.domain.LogQueryFilter
import com.example.zhttaskflow.feature.log.domain.repository.LogRepository
import java.io.File

/**
 * 按筛选条件导出日志用例。
 */
class ExportLogsUseCase(
    private val repository: LogRepository,
) {

    suspend operator fun invoke(
        filter: LogQueryFilter,
        maxEntries: Int = 2_000,
    ): File {
        return repository.exportLogs(filter, maxEntries)
    }
}
