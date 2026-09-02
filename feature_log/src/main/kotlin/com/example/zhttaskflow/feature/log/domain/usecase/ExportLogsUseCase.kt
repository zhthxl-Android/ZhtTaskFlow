package com.example.zhttaskflow.feature.log.domain.usecase

import com.example.zhttaskflow.feature.log.domain.LogExportScope
import com.example.zhttaskflow.feature.log.domain.LogQueryFilter
import com.example.zhttaskflow.feature.log.domain.repository.LogRepository
import java.io.File

/**
 * 按筛选条件导出日志用例。
 *
 * @param listFilter 当前列表顶部类型筛选（全部/埋点/性能/崩溃）
 * @param scope [LogExportScope.CURRENT_FILTER] 时与列表一致；[LogExportScope.ALL_LOGS] 时忽略类型筛选
 */
class ExportLogsUseCase(
    private val repository: LogRepository,
) {

    suspend operator fun invoke(
        listFilter: LogQueryFilter,
        scope: LogExportScope = LogExportScope.CURRENT_FILTER,
        maxEntries: Int = 2_000,
    ): File {
        val effectiveFilter = when (scope) {
            LogExportScope.CURRENT_FILTER -> listFilter
            LogExportScope.ALL_LOGS -> LogQueryFilter(logType = null)
        }
        return repository.exportLogs(effectiveFilter, maxEntries)
    }
}
