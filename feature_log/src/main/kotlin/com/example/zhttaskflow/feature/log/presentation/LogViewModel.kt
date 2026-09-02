package com.example.zhttaskflow.feature.log.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.mvi.BaseViewModel
import com.example.zhttaskflow.feature.log.R
import com.example.zhttaskflow.feature.log.domain.LogCategory
import com.example.zhttaskflow.feature.log.domain.LogExportScope
import com.example.zhttaskflow.feature.log.domain.LogEntry
import com.example.zhttaskflow.feature.log.domain.LogQueryFilter
import com.example.zhttaskflow.feature.log.domain.usecase.ClearLogsUseCase
import com.example.zhttaskflow.feature.log.domain.usecase.ExportLogsUseCase
import com.example.zhttaskflow.feature.log.domain.usecase.LogDisplayUseCase
import com.example.zhttaskflow.feature.log.domain.usecase.QueryLogsUseCase

/** 日志查看页埋点 pageId（与 [PageLifecycleLog] 一致）。 */
internal const val LOG_PAGE_ID: String = "LogViewer"

private const val LOG_PAGE_SIZE: Int = 50
private const val LOG_EXPORT_MAX_ENTRIES: Int = 2_000

/**
 * 日志查看 ViewModel：索引分页查询、展开懒加载详情、导出与清空。
 */
internal class LogViewModel(
    private val appContext: Context,
    private val queryLogsUseCase: QueryLogsUseCase,
    private val exportLogsUseCase: ExportLogsUseCase,
    private val clearLogsUseCase: ClearLogsUseCase,
    private val logDisplayUseCase: LogDisplayUseCase,
) : BaseViewModel<LogUiState, LogUiEvent, LogUiEffect>(BaseUiState.Loading) {

    private var currentFilter: LogTypeFilter = LogTypeFilter.ALL
    private var expandedEntryIds: Set<String> = emptySet()
    /** 仅持有当前已加载页对应的记录，用于展开详情。 */
    private var recordByEntryId: Map<String, LogEntry> = emptyMap()
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
            LogUiEvent.Refresh -> loadFirstPage(showLoading = false, isRefresh = true)
            is LogUiEvent.FilterSelected -> {
                if (currentFilter != event.filter) {
                    currentFilter = event.filter
                    releaseMemoryCaches()
                    loadFirstPage(showLoading = false)
                }
            }
            is LogUiEvent.EntryToggled -> toggleExpanded(event.entryId)
            LogUiEvent.LoadMore -> loadNextPage()
            is LogUiEvent.ExportConfirmed -> exportLogs(event.scope)
            LogUiEvent.ClearRequested -> {
                // 二次确认在 Screen 层完成
            }
            LogUiEvent.ClearConfirmed -> clearAllLogs()
        }
    }

    private fun loadFirstPage(showLoading: Boolean = true, isRefresh: Boolean = false) {
        loadedPageCount = 0
        hasMorePages = false
        when {
            showLoading && !isRefresh -> setState { BaseUiState.Loading }
            isRefresh -> applyRefreshingFlag(isRefreshing = true)
        }
        launchTask(
            tag = "LogViewModel",
            scene = "loadFirstPage",
            precheckNetwork = false,
            onError = { _, userMessage ->
                applyRefreshingFlag(isRefreshing = false)
                setState { BaseUiState.Error(userMessage) }
            },
        ) {
            val pageResult = queryLogsUseCase(
                filter = currentFilter.toQueryFilter(),
                page = 0,
                pageSize = LOG_PAGE_SIZE,
            )
            applyPageResult(
                newEntries = pageResult.entries,
                hasMore = pageResult.hasMore,
                append = false,
                showLoading = showLoading,
                isRefresh = isRefresh,
            )
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
            val pageResult = queryLogsUseCase(
                filter = currentFilter.toQueryFilter(),
                page = nextPage,
                pageSize = LOG_PAGE_SIZE,
            )
            applyPageResult(
                newEntries = pageResult.entries,
                hasMore = pageResult.hasMore,
                append = true,
                showLoading = false,
                isRefresh = false,
            )
        }
    }

    private fun applyRefreshingFlag(isRefreshing: Boolean) {
        when (val state = currentState) {
            is BaseUiState.Success -> {
                setState { BaseUiState.Success(state.data.copy(isRefreshing = isRefreshing)) }
            }
            BaseUiState.Empty -> {
                setState {
                    BaseUiState.Success(
                        LogData(
                            filter = currentFilter,
                            entries = emptyList(),
                            expandedEntryIds = emptySet(),
                            isRefreshing = isRefreshing,
                        ),
                    )
                }
            }
            else -> Unit
        }
    }

    private fun applyPageResult(
        newEntries: List<LogEntry>,
        hasMore: Boolean,
        append: Boolean,
        @Suppress("UNUSED_PARAMETER") showLoading: Boolean,
        @Suppress("UNUSED_PARAMETER") isRefresh: Boolean,
    ) {
        isLoadingMore = false
        val previousIds = if (append) {
            (currentState as? BaseUiState.Success)?.data?.entries?.map { entry -> entry.id }.orEmpty()
        } else {
            emptyList()
        }
        if (!append) {
            releaseMemoryCaches()
            loadedPageCount = 0
        } else if (newEntries.isNotEmpty()) {
            entryDetailCache = emptyMap()
            expandedEntryIds = emptySet()
        }
        val mergedRecords = if (append) {
            val orderedExisting = previousIds.mapNotNull { id -> recordByEntryId[id] }
            orderedExisting + newEntries
        } else {
            newEntries
        }
        recordByEntryId = mergedRecords.associateBy { record -> record.id }
        trimDetailCacheToLoadedEntries()
        if (newEntries.isNotEmpty()) {
            loadedPageCount += 1
        }
        hasMorePages = hasMore
        val entries = mergedRecords.map { record -> mapRecordToUi(record) }
        val data = buildLogData(entries).copy(isRefreshing = false)
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
                entryDetailCache = entryDetailCache + (entryId to logDisplayUseCase.encodeDetail(record))
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

    private fun exportLogs(scope: LogExportScope) {
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
            val exportFile = exportLogsUseCase(
                listFilter = currentFilter.toQueryFilter(),
                scope = scope,
                maxEntries = LOG_EXPORT_MAX_ENTRIES,
            )
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
            clearLogsUseCase()
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

    private fun mapRecordToUi(record: LogEntry): LogEntryUi {
        return LogEntryUi(
            id = record.id,
            timestampText = logDisplayUseCase.formatTimestamp(record.timestampEpochMs),
            typeLabel = record.typeLabel,
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

    private fun buildSummary(record: LogEntry): String {
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

private fun LogTypeFilter.toQueryFilter(): LogQueryFilter {
    return LogQueryFilter(logType = toLogCategory())
}

private fun LogTypeFilter.toLogCategory(): LogCategory? {
    return when (this) {
        LogTypeFilter.ALL -> null
        LogTypeFilter.ANALYTICS -> LogCategory.ANALYTICS
        LogTypeFilter.PERFORMANCE -> LogCategory.PERFORMANCE
        LogTypeFilter.CRASH -> LogCategory.CRASH
    }
}

internal typealias LogUiState = BaseUiState<LogData>

/**
 * [LogViewModel] 工厂。
 */
internal class LogViewModelFactory(
    private val appContext: Context,
    private val queryLogsUseCase: QueryLogsUseCase,
    private val exportLogsUseCase: ExportLogsUseCase,
    private val clearLogsUseCase: ClearLogsUseCase,
    private val logDisplayUseCase: LogDisplayUseCase,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogViewModel::class.java)) {
            return LogViewModel(
                appContext = appContext.applicationContext,
                queryLogsUseCase = queryLogsUseCase,
                exportLogsUseCase = exportLogsUseCase,
                clearLogsUseCase = clearLogsUseCase,
                logDisplayUseCase = logDisplayUseCase,
            ) as T
        }
        throw IllegalArgumentException("未知 ViewModel: ${modelClass.name}")
    }
}
