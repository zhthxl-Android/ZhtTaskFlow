package com.example.zhttaskflow.feature.log.data.mapper

import com.example.zhttaskflow.core.observability.TaskFlowLocalLogStore
import com.example.zhttaskflow.feature.log.domain.LogCategory
import com.example.zhttaskflow.feature.log.domain.LogEntry
import com.example.zhttaskflow.feature.log.domain.LogQueryFilter

internal object LogRecordMapper {

    fun toDomain(record: TaskFlowLocalLogStore.LogRecord): LogEntry {
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

    fun toStore(entry: LogEntry): TaskFlowLocalLogStore.LogRecord {
        return TaskFlowLocalLogStore.LogRecord(
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

    fun toStoreLogType(category: LogCategory?): TaskFlowLocalLogStore.LogType? {
        return category?.toStore()
    }

    fun toStoreQueryFilter(filter: LogQueryFilter, maxEntries: Int): TaskFlowLocalLogStore.QueryFilter {
        return TaskFlowLocalLogStore.QueryFilter(
            logType = toStoreLogType(filter.logType),
            maxEntries = maxEntries,
        )
    }

    private fun TaskFlowLocalLogStore.LogType.toDomain(): LogCategory {
        return when (this) {
            TaskFlowLocalLogStore.LogType.ANALYTICS -> LogCategory.ANALYTICS
            TaskFlowLocalLogStore.LogType.PERFORMANCE -> LogCategory.PERFORMANCE
            TaskFlowLocalLogStore.LogType.CRASH -> LogCategory.CRASH
        }
    }

    private fun LogCategory.toStore(): TaskFlowLocalLogStore.LogType {
        return when (this) {
            LogCategory.ANALYTICS -> TaskFlowLocalLogStore.LogType.ANALYTICS
            LogCategory.PERFORMANCE -> TaskFlowLocalLogStore.LogType.PERFORMANCE
            LogCategory.CRASH -> TaskFlowLocalLogStore.LogType.CRASH
        }
    }
}
