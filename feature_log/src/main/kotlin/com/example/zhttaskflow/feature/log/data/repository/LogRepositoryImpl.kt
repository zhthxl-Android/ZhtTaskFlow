package com.example.zhttaskflow.feature.log.data.repository

import android.content.Context
import com.example.zhttaskflow.core.observability.LocalLogStore
import com.example.zhttaskflow.feature.log.data.mapper.LogRecordMapper
import com.example.zhttaskflow.feature.log.domain.LogEntry
import com.example.zhttaskflow.feature.log.domain.LogPage
import com.example.zhttaskflow.feature.log.domain.LogQueryFilter
import com.example.zhttaskflow.feature.log.domain.repository.LogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * [LogRepository] 实现：委托 [LocalLogStore] 完成本地读写。
 */
class LogRepositoryImpl(
    private val appContext: Context,
) : LogRepository {

    override suspend fun queryPaged(
        filter: LogQueryFilter,
        page: Int,
        pageSize: Int,
    ): LogPage = withContext(Dispatchers.IO) {
        val result = LocalLogStore.queryPaged(
            LocalLogStore.PagedQueryFilter(
                logType = LogRecordMapper.toStoreLogType(filter.logType),
                page = page,
                pageSize = pageSize,
            ),
        )
        LogPage(
            entries = result.records.map(LogRecordMapper::toDomain),
            hasMore = result.hasMore,
        )
    }

    override suspend fun exportLogs(filter: LogQueryFilter, maxEntries: Int): File =
        withContext(Dispatchers.IO) {
            val storeFilter = LogRecordMapper.toStoreQueryFilter(filter, maxEntries)
            LocalLogStore.exportRecentLogs(appContext, storeFilter, maxEntries)
        }

    override suspend fun clearAllLogs() {
        withContext(Dispatchers.IO) {
            LocalLogStore.clearAllLogs()
        }
    }

    override fun formatTimestamp(epochMs: Long): String {
        return LocalLogStore.formatTimestamp(epochMs)
    }

    override fun encodeLogDetail(entry: LogEntry): String {
        return LocalLogStore.encodeRecord(LogRecordMapper.toStore(entry))
    }
}
