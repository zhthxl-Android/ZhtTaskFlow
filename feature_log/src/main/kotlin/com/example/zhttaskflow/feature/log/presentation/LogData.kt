package com.example.zhttaskflow.feature.log.presentation

/**
 * 日志查看页成功态数据。
 */
data class LogData(
    val filter: LogTypeFilter,
    val entries: List<LogEntryUi>,
    val expandedEntryIds: Set<String>,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
)

/**
 * 顶部类型筛选（与 [com.example.zhttaskflow.core.observability.TaskFlowLocalLogStore.LogType] 对应）。
 */
enum class LogTypeFilter {
    ALL,
    ANALYTICS,
    PERFORMANCE,
    CRASH,
}

/**
 * 单条日志列表项 UI 模型。
 */
data class LogEntryUi(
    val id: String,
    val timestampText: String,
    val typeLabel: String,
    val pageId: String,
    val actionId: String,
    val summary: String,
    /** 展开后懒加载的 JSON 详情；未展开时为 null。 */
    val detailText: String? = null,
)
