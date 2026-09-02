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

/**
 * 日志查看 ViewModel：本地日志查询、筛选、导出、清空。
 */
internal class LogViewModel(
    private val appContext: Context,
) : BaseViewModel<LogUiState, LogUiEvent, LogUiEffect>(BaseUiState.Loading) {

    private var currentFilter: LogTypeFilter = LogTypeFilter.ALL
    private var expandedEntryIds: Set<String> = emptySet()

    init {
        onEvent(LogUiEvent.Load)
    }

    override fun handleEvent(event: LogUiEvent) {
        when (event) {
            LogUiEvent.Load,
            LogUiEvent.Retry,
            -> loadLogs()
            is LogUiEvent.FilterSelected -> {
                if (currentFilter != event.filter) {
                    currentFilter = event.filter
                    expandedEntryIds = emptySet()
                    loadLogs(showLoading = false)
                }
            }
            is LogUiEvent.EntryToggled -> toggleExpanded(event.entryId)
            LogUiEvent.ExportRequested -> exportLogs()
            LogUiEvent.ClearRequested -> {
                // 二次确认在 Screen 层完成
            }
            LogUiEvent.ClearConfirmed -> clearAllLogs()
        }
    }

    private fun loadLogs(showLoading: Boolean = true) {
        if (showLoading) {
            setState { BaseUiState.Loading }
        }
        launchTask(
            tag = "LogViewModel",
            scene = "loadLogs",
            precheckNetwork = false,
            onError = { _, userMessage ->
                setState { BaseUiState.Error(userMessage) }
            },
        ) {
            val entries = withContext(Dispatchers.IO) {
                queryEntries(currentFilter)
            }
            val data = LogData(
                filter = currentFilter,
                entries = entries,
                expandedEntryIds = expandedEntryIds,
            )
            setState {
                if (entries.isEmpty()) {
                    BaseUiState.Empty
                } else {
                    BaseUiState.Success(data)
                }
            }
        }
    }

    private fun toggleExpanded(entryId: String) {
        val current = currentState
        val data = (current as? BaseUiState.Success)?.data ?: return
        expandedEntryIds = if (entryId in expandedEntryIds) {
            expandedEntryIds - entryId
        } else {
            expandedEntryIds + entryId
        }
        setState {
            BaseUiState.Success(
                data.copy(expandedEntryIds = expandedEntryIds),
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
            expandedEntryIds = emptySet()
            val message = appContext.getString(R.string.log_str_clear_success)
            sendEffect(LogUiEffect.ShowSnackbar(message = message))
            loadLogs(showLoading = false)
        }
    }

    /**
     * 供空态 / 加载态顶栏筛选展示当前选中项。
     */
    fun currentFilterForUi(): LogTypeFilter = currentFilter

    private fun queryEntries(filter: LogTypeFilter): List<LogEntryUi> {
        val storeFilter = TaskFlowLocalLogStore.QueryFilter(
            logType = filter.toStoreLogType(),
            maxEntries = 2_000,
        )
        return TaskFlowLocalLogStore.query(storeFilter).map { record ->
            val summary = buildSummary(record)
            LogEntryUi(
                id = record.stableId(),
                timestampText = TaskFlowLocalLogStore.formatTimestamp(record.timestampEpochMs),
                typeLabel = record.logType.displayName,
                pageId = record.pageId.ifBlank { "—" },
                actionId = record.actionId.ifBlank { "—" },
                summary = summary,
                detailText = TaskFlowLocalLogStore.encodeRecord(record),
            )
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
