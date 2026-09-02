package com.example.zhttaskflow.feature.log.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.mvi.BaseViewModel
import com.example.zhttaskflow.core.observability.TaskFlowLocalLogStore
import com.example.zhttaskflow.feature.log.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 日志查看页埋点 pageId（与 [PageLifecycleLog] 一致）。 */
internal const val LOG_PAGE_ID: String = "LogViewer"

private const val LOG_PAGE_SIZE: Int = 50

/**
 * 日志查看 ViewModel：索引分页查询、展开懒加载详情、导出与清空。
 */
internal class LogViewModel(
    private val appContext: Context,
) : BaseViewModel<LogUiState, LogUiEvent, LogUiEffect>(BaseUiState.Loading) {

    private var currentFilter: LogTypeFilter = LogTypeFilter.ALL
    private var expandedEntryIds: Set<String> = emptySet()
    /** 仅持有当前已加载页对应的记录，用于展开详情。 */
    private var recordByEntryId: Map<String, TaskFlowLocalLogStore.LogRecord> = emptyMap()
    /** 详情 JSON 缓存，仅保留当前已加载列表中的条目。 */
    private var entryDetailCache: Map<String, String> = emptyMap()
    private var loadedPageCount: Int = 0
    private var hasMorePages: Boolean = false
    private var isLoadingMore: Boolean = false

    init {
        onEvent(LogUiEvent.Load)
    }

    override fun onCleared() {
        releaseMemoryCaches()
        super.onCleared()
    }

    override fun handleEvent(event: LogUiEvent) {
        when (event) {
            LogUiEvent.Load,
            LogUiEvent.Retry,
            -> loadFirstPage()
            is LogUiEvent.FilterSelected -> {
                if (currentFilter != event.filter) {
                    currentFilter = event.filter
                    releaseMemoryCaches()
                    loadFirstPage(showLoading = false)
                }
            }
            is LogUiEvent.EntryToggled -> toggleExpanded(event.entryId)
            LogUiEvent.LoadMore -> loadNextPage()
            LogUiEvent.ExportRequested -> exportLogs()
            LogUiEvent.ClearRequested -> {
                // 二次确认在 Screen 层完成
            }
            LogUiEvent.ClearConfirmed -> clearAllLogs()
        }
    }

    private fun loadFirstPage(showLoading: Boolean = true) {
        loadedPageCount = 0
        hasMorePages = false
        if (showLoading) {
            setState { BaseUiState.Loading }
        }
        launchTask(
            tag = "LogViewModel",
            scene = "loadFirstPage",
            precheckNetwork = false,
            onError = { _, userMessage ->
                setState { BaseUiState.Error(userMessage) }
            },
        ) {
            val pageResult = withContext(Dispatchers.IO) {
                fetchPage(page = 0)
            }
            applyPageResult(pageResult, append = false, showLoading = showLoading)
        }
    }

    private fun loadNextPage() {
        if (!hasMorePages || isLoadingMore) {
            return
        }
        val current = currentState as? BaseUiState.Success ?: return
        isLoadingMore = true
        setState {
            BaseUiState.Success(current.data.copy(isLoadingMore = true))
        }
        launchTask(
            tag = "LogViewModel",
            scene = "loadNextPage",
            precheckNetwork = false,
            onError = { _, _ ->
                isLoadingMore = false
                val success = currentState as? BaseUiState.Success ?: return@launchTask
                setState {
                    BaseUiState.Success(success.data.copy(isLoadingMore = false))
                }
            },
        ) {
            val nextPage = loadedPageCount
            val pageResult = withContext(Dispatchers.IO) {
                fetchPage(page = nextPage)
            }
            applyPageResult(pageResult, append = true, showLoading = false)
        }
    }

    private fun fetchPage(page: Int): TaskFlowLocalLogStore.PagedQueryResult {
        return TaskFlowLocalLogStore.queryPaged(
            TaskFlowLocalLogStore.PagedQueryFilter(
                logType = currentFilter.toStoreLogType(),
                page = page,
                pageSize = LOG_PAGE_SIZE,
            ),
        )
    }

    private fun applyPageResult(
        pageResult: TaskFlowLocalLogStore.PagedQueryResult,
        append: Boolean,
        @Suppress("UNUSED_PARAMETER") showLoading: Boolean,
    ) {
        isLoadingMore = false
        val newRecords = pageResult.records
        val previousIds = if (append) {
            (currentState as? BaseUiState.Success)?.data?.entries?.map { entry -> entry.id }.orEmpty()
        } else {
            emptyList()
        }
        if (!append) {
            releaseMemoryCaches()
            loadedPageCount = 0
        } else if (newRecords.isNotEmpty()) {
            entryDetailCache = emptyMap()
            expandedEntryIds = emptySet()
        }
        val mergedRecords = if (append) {
            val orderedExisting = previousIds.mapNotNull { id -> recordByEntryId[id] }
            orderedExisting + newRecords
        } else {
            newRecords
        }
        recordByEntryId = mergedRecords.associateBy { record -> record.stableId() }
        trimDetailCacheToLoadedEntries()
        if (newRecords.isNotEmpty()) {
            loadedPageCount += 1
        }
        hasMorePages = pageResult.hasMore
        val entries = mergedRecords.map { record -> mapRecordToUi(record) }
        val data = buildLogData(entries)
        setState {
            when {
                entries.isEmpty() && !append -> BaseUiState.Empty
                else -> BaseUiState.Success(data)
            }
        }
    }

    private fun toggleExpanded(entryId: String) {
        val current = currentState
        val data = (current as? BaseUiState.Success)?.data ?: return
        val willExpand = entryId !in expandedEntryIds
        expandedEntryIds = if (willExpand) {
            expandedEntryIds + entryId
        } else {
            expandedEntryIds - entryId
        }
        if (willExpand) {
            val record = recordByEntryId[entryId]
            if (record != null && entryId !in entryDetailCache) {
                entryDetailCache = entryDetailCache + (entryId to TaskFlowLocalLogStore.encodeRecord(record))
            }
        }
        setState {
            BaseUiState.Success(
                data.copy(
                    entries = applyDetailsToEntries(data.entries),
                    expandedEntryIds = expandedEntryIds,
                ),
            )
        }
    }

    private fun exportLogs() {
        launchTask(
            tag = "LogViewModel",
            scene = "exportLogs",
            precheckNetwork = false,
            onError = { _, userMessage ->
                sendEffect(
                    LogUiEffect.ShowSnackbar(
                        message = userMessage,
                        type = com.example.zhttaskflow.base.ext.SnackbarType.Error,
                    ),
                )
            },
        ) {
            val exportFile = withContext(Dispatchers.IO) {
                TaskFlowLocalLogStore.exportRecentLogs(appContext)
            }
            val chooserTitle = appContext.getString(R.string.log_str_export_share_title)
            sendEffect(
                LogUiEffect.ShareLogExport(exportFile = exportFile, chooserTitle = chooserTitle),
            )
        }
    }

    private fun clearAllLogs() {
        launchTask(
            tag = "LogViewModel",
            scene = "clearAllLogs",
            precheckNetwork = false,
            onError = { _, userMessage ->
                sendEffect(
                    LogUiEffect.ShowSnackbar(
                        message = userMessage,
                        type = com.example.zhttaskflow.base.ext.SnackbarType.Error,
                    ),
                )
            },
        ) {
            withContext(Dispatchers.IO) {
                TaskFlowLocalLogStore.clearAllLogs()
            }
            releaseMemoryCaches()
            loadedPageCount = 0
            hasMorePages = false
            val message = appContext.getString(R.string.log_str_clear_success)
            sendEffect(LogUiEffect.ShowSnackbar(message = message))
            loadFirstPage(showLoading = false)
        }
    }

    fun currentFilterForUi(): LogTypeFilter = currentFilter

    private fun releaseMemoryCaches() {
        recordByEntryId = emptyMap()
        entryDetailCache = emptyMap()
        expandedEntryIds = emptySet()
    }

    private fun trimDetailCacheToLoadedEntries() {
        val allowedIds = recordByEntryId.keys
        entryDetailCache = entryDetailCache.filterKeys { id -> id in allowedIds }
        expandedEntryIds = expandedEntryIds.intersect(allowedIds)
    }

    private fun mapRecordToUi(record: TaskFlowLocalLogStore.LogRecord): LogEntryUi {
        return LogEntryUi(
            id = record.stableId(),
            timestampText = TaskFlowLocalLogStore.formatTimestamp(record.timestampEpochMs),
            typeLabel = record.logType.displayName,
            pageId = record.pageId.ifBlank { "—" },
            actionId = record.actionId.ifBlank { "—" },
            summary = buildSummary(record),
        )
    }

    private fun buildLogData(entries: List<LogEntryUi>): LogData {
        return LogData(
            filter = currentFilter,
            entries = applyDetailsToEntries(entries),
            expandedEntryIds = expandedEntryIds,
            hasMore = hasMorePages,
            isLoadingMore = isLoadingMore,
        )
    }

    private fun applyDetailsToEntries(entries: List<LogEntryUi>): List<LogEntryUi> {
        return entries.map { entry ->
            if (entry.id in expandedEntryIds) {
                entry.copy(detailText = entryDetailCache[entry.id])
            } else {
                entry.copy(detailText = null)
            }
        }
    }

    private fun buildSummary(record: TaskFlowLocalLogStore.LogRecord): String {
        val paramsPreview = record.params.entries
            .take(3)
            .joinToString(separator = ", ") { (key, value) -> "$key=$value" }
        return buildString {
            append(record.event)
            if (paramsPreview.isNotBlank()) {
                append(" · ")
                append(paramsPreview)
            }
            if (record.params.size > 3) {
                append(" …")
            }
        }
    }
}

private fun LogTypeFilter.toStoreLogType(): TaskFlowLocalLogStore.LogType? {
    return when (this) {
        LogTypeFilter.ALL -> null
        LogTypeFilter.ANALYTICS -> TaskFlowLocalLogStore.LogType.ANALYTICS
        LogTypeFilter.PERFORMANCE -> TaskFlowLocalLogStore.LogType.PERFORMANCE
        LogTypeFilter.CRASH -> TaskFlowLocalLogStore.LogType.CRASH
    }
}

internal typealias LogUiState = BaseUiState<LogData>

/**
 * [LogViewModel] 工厂。
 */
internal class LogViewModelFactory(
    private val appContext: Context,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogViewModel::class.java)) {
            return LogViewModel(appContext.applicationContext) as T
        }
        throw IllegalArgumentException("未知 ViewModel: ${modelClass.name}")
    }
}
