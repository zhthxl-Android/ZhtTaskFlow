package com.example.zhttaskflow.feature.log.domain

/**
 * 日志类型（与本地仓 wire 类型一一对应，领域层不依赖 core 枚举）。
 */
enum class LogCategory {
    ANALYTICS,
    PERFORMANCE,
    CRASH,
}

/**
 * 分页查询筛选条件。
 */
data class LogQueryFilter(
    val logType: LogCategory? = null,
)

/**
 * 单条可观测日志领域实体。
 */
data class LogEntry(
    val id: String,
    val timestampEpochMs: Long,
    val category: LogCategory,
    val typeLabel: String,
    val pageId: String,
    val actionId: String,
    val event: String,
    val params: Map<String, String>,
    val stackTrace: String? = null,
    val deviceInfo: String? = null,
    val anomaly: Boolean = false,
)

/**
 * 分页查询结果。
 */
data class LogPage(
    val entries: List<LogEntry>,
    val hasMore: Boolean,
)
