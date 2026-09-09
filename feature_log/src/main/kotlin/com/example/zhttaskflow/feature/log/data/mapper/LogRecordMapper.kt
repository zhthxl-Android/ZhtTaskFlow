package com.example.zhttaskflow.feature.log.data.mapper

import com.example.zhttaskflow.core.observability.LocalLogStore
import com.example.zhttaskflow.feature.log.domain.LogCategory
import com.example.zhttaskflow.feature.log.domain.LogEntry
import com.example.zhttaskflow.feature.log.domain.LogQueryFilter

internal object LogRecordMapper {

    fun toDomain(record: LocalLogStore.LogRecord): LogEntry {
        return LogEntry(
            id = record.stableId(),
            timestampEpochMs = record.timestampEpochMs,
            category = record.logType.toDomain(),
            typeLabel = record.logType.displayName,
            pageId = record.pageId,
            actionId = record.actionId,
            event = record.event,
            params = record.params,
            stackTrace = record.stackTrace,
            deviceInfo = record.deviceInfo,
            anomaly = record.anomaly,
        )
    }

    fun toStore(entry: LogEntry): LocalLogStore.LogRecord {
        return LocalLogStore.LogRecord(
            timestampEpochMs = entry.timestampEpochMs,
            logType = entry.category.toStore(),
            pageId = entry.pageId,
            actionId = entry.actionId,
            event = entry.event,
            params = entry.params,
            stackTrace = entry.stackTrace,
            deviceInfo = entry.deviceInfo,
            anomaly = entry.anomaly,
        )
    }

    fun toStoreLogType(category: LogCategory?): LocalLogStore.LogType? {
        return category?.toStore()
    }

    fun toStoreQueryFilter(filter: LogQueryFilter, maxEntries: Int): LocalLogStore.QueryFilter {
        return LocalLogStore.QueryFilter(
            logType = toStoreLogType(filter.logType),
            maxEntries = maxEntries,
        )
    }

    private fun LocalLogStore.LogType.toDomain(): LogCategory {
        return when (this) {
            LocalLogStore.LogType.ANALYTICS -> LogCategory.ANALYTICS
            LocalLogStore.LogType.PERFORMANCE -> LogCategory.PERFORMANCE
            LocalLogStore.LogType.CRASH -> LogCategory.CRASH
        }
    }

    private fun LogCategory.toStore(): LocalLogStore.LogType {
        return when (this) {
            LogCategory.ANALYTICS -> LocalLogStore.LogType.ANALYTICS
            LogCategory.PERFORMANCE -> LocalLogStore.LogType.PERFORMANCE
            LogCategory.CRASH -> LocalLogStore.LogType.CRASH
        }
    }
}
