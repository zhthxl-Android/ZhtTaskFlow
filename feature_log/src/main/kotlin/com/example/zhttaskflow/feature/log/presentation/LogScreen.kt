package com.example.zhttaskflow.feature.log.presentation

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.ext.TaskFlowDialogController
import com.example.zhttaskflow.base.ext.rememberTaskFlowDialogController
import com.example.zhttaskflow.base.ext.rememberTaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ext.showSnackbar
import com.example.zhttaskflow.base.extension.collectUiStateWithLifecycle
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.ui.StateBox
import com.example.zhttaskflow.base.ui.TaskFlowListScaffold
import com.example.zhttaskflow.base.ui.extension.PageLifecycleLog
import com.example.zhttaskflow.base.ui.extension.logUiInteraction
import com.example.zhttaskflow.base.ui.extension.logUiOutcome
import com.example.zhttaskflow.base.ui.rememberTaskFlowStateBoxContentPadding
import com.example.zhttaskflow.base.ui.state.BaseEmptyScreen
import com.example.zhttaskflow.feature.log.domain.LogExportScope
import com.example.zhttaskflow.feature.log.R

/**
 * 日志查看 Tab 页：筛选、列表、导出分享、清空。
 */
@Composable
internal fun LogScreen(
    viewModel: LogViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectUiStateWithLifecycle()
    val dialogController = rememberTaskFlowDialogController()
    val snackbarDispatcher = rememberTaskFlowSnackbarDispatcher()
    val context = LocalContext.current

    val lifecycleArgs = when (uiState) {
        is BaseUiState.Empty -> "empty;filter=${viewModel.currentFilterForUi().name}"
        is BaseUiState.Loading -> "loading"
        is BaseUiState.Error -> "error"
        is BaseUiState.Success -> {
            val data = (uiState as BaseUiState.Success).data
            "count=${data.entries.size};hasMore=${data.hasMore};filter=${data.filter.name}"
        }
    }

    PageLifecycleLog(
        pageName = LOG_PAGE_ID,
        pageArgs = lifecycleArgs,
    )

    val clearTitle = stringResource(id = R.string.log_str_clear_confirm_title)
    val clearMessage = stringResource(id = R.string.log_str_clear_confirm_message)
    val confirmText = stringResource(id = R.string.log_str_confirm)
    val dismissText = stringResource(id = R.string.log_str_cancel)
    val emptyMessage = stringResource(id = R.string.log_str_empty)
    val exportDialogTitle = stringResource(id = R.string.log_str_export_dialog_title)
    val exportDialogMessage = stringResource(id = R.string.log_str_export_dialog_message)
    val exportScopeFiltered = stringResource(id = R.string.log_str_export_scope_filtered)
    val exportScopeAll = stringResource(id = R.string.log_str_export_scope_all)

    LaunchedEffect(viewModel, snackbarDispatcher, context) {
        viewModel.uiEffect.collect { effect ->
            consumeLogUiEffect(
                context = context,
                dispatcher = snackbarDispatcher,
                effect = effect,
            )
        }
    }

    TaskFlowListScaffold(
        modifier = modifier,
        title = stringResource(id = R.string.log_str_viewer_title),
        interceptTabRootBackToDesktop = false,
        actions = {
            TextButton(
                onClick = {
                    logUiInteraction(
                        action = "click",
                        identifier = "log_export",
                        pageId = LOG_PAGE_ID,
                    )
                    showLogExportDialog(
                        dialogController = dialogController,
                        title = exportDialogTitle,
                        message = exportDialogMessage,
                        filteredLabel = exportScopeFiltered,
                        allLabel = exportScopeAll,
                        cancelLabel = dismissText,
                        onScopeSelected = { scope ->
                            viewModel.onEvent(LogUiEvent.ExportConfirmed(scope))
                        },
                    )
                },
            ) {
                Text(text = stringResource(id = R.string.log_str_export))
            }
            TextButton(
                onClick = {
                    logUiInteraction(
                        action = "click",
                        identifier = "log_clear_request",
                        pageId = LOG_PAGE_ID,
                    )
                    dialogController.showConfirmDialog(
                        title = clearTitle,
                        message = clearMessage,
                        confirmText = confirmText,
                        dismissText = dismissText,
                        onConfirm = {
                            logUiInteraction(
                                action = "click",
                                identifier = "log_clear_confirm",
                                pageId = LOG_PAGE_ID,
                            )
                            viewModel.onEvent(LogUiEvent.ClearConfirmed)
                        },
                    )
                },
            ) {
                Text(text = stringResource(id = R.string.log_str_clear))
            }
        },
    ) { scaffoldContentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldContentPadding),
        ) {
            val selectedFilter = when (val state = uiState) {
                is BaseUiState.Success -> state.data.filter
                else -> viewModel.currentFilterForUi()
            }
            LogTypeFilterRow(
                selected = selectedFilter,
                onSelected = { filter ->
                    logUiInteraction(
                        action = "click",
                        identifier = "log_filter_${filter.name.lowercase()}",
                        pageId = LOG_PAGE_ID,
                    )
                    viewModel.onEvent(LogUiEvent.FilterSelected(filter))
                },
            )
            StateBox(
                uiState = uiState,
                onRetry = {
                    logUiInteraction(
                        action = "click",
                        identifier = "log_retry",
                        pageId = LOG_PAGE_ID,
                    )
                    viewModel.onEvent(LogUiEvent.Retry)
                },
                emptyMessage = emptyMessage,
                contentPadding = rememberTaskFlowStateBoxContentPadding(),
                modifier = Modifier.fillMaxSize(),
            ) { data ->
                LogEntryList(
                    entries = data.entries,
                    expandedEntryIds = data.expandedEntryIds,
                    hasMore = data.hasMore,
                    isLoadingMore = data.isLoadingMore,
                    listContentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    onLoadMore = {
                        logUiInteraction(
                            action = "scroll",
                            identifier = "log_load_more",
                            pageId = LOG_PAGE_ID,
                        )
                        viewModel.onEvent(LogUiEvent.LoadMore)
                    },
                    onEntryClick = { entryId ->
                        logUiInteraction(
                            action = "click",
                            identifier = "log_entry_toggle",
                            pageId = LOG_PAGE_ID,
                            params = mapOf("entryId" to entryId),
                        )
                        viewModel.onEvent(LogUiEvent.EntryToggled(entryId))
                    },
                )
            }
        }
    }
}

/**
 * 导出范围选择：默认推荐「当前筛选结果」（置于首位）。
 */
private fun showLogExportDialog(
    dialogController: TaskFlowDialogController,
    title: String,
    message: String,
    filteredLabel: String,
    allLabel: String,
    cancelLabel: String,
    onScopeSelected: (LogExportScope) -> Unit,
) {
    dialogController.showBottomSheet {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
            TextButton(
                onClick = {
                    dialogController.dismissAll()
                    onScopeSelected(LogExportScope.CURRENT_FILTER)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = filteredLabel)
            }
            TextButton(
                onClick = {
                    dialogController.dismissAll()
                    onScopeSelected(LogExportScope.ALL_LOGS)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = allLabel)
            }
            TextButton(
                onClick = { dialogController.dismissAll() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = cancelLabel)
            }
        }
    }
}

@Composable
private fun LogTypeFilterRow(
    selected: LogTypeFilter,
    onSelected: (LogTypeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LogTypeFilter.entries.forEach { filter ->
            val label = when (filter) {
                LogTypeFilter.ALL -> stringResource(id = R.string.log_str_filter_all)
                LogTypeFilter.ANALYTICS -> stringResource(id = R.string.log_str_filter_analytics)
                LogTypeFilter.PERFORMANCE -> stringResource(id = R.string.log_str_filter_performance)
                LogTypeFilter.CRASH -> stringResource(id = R.string.log_str_filter_crash)
            }
            FilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = { Text(text = label) },
            )
        }
    }
}

@Composable
private fun LogEntryList(
    entries: List<LogEntryUi>,
    expandedEntryIds: Set<String>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    listContentPadding: PaddingValues,
    onLoadMore: () -> Unit,
    onEntryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, entries.size, hasMore, isLoadingMore) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to layoutInfo.totalItemsCount
        }.collect { (lastVisible, totalItems) ->
            if (hasMore && !isLoadingMore && totalItems > 0 && lastVisible >= totalItems - 3) {
                onLoadMore()
            }
        }
    }
    if (entries.isEmpty()) {
        BaseEmptyScreen(
            message = stringResource(id = R.string.log_str_empty),
            modifier = modifier.fillMaxSize(),
        )
        return
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = listContentPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(entries, key = { item -> item.id }) { entry ->
            val expanded = entry.id in expandedEntryIds
            LogEntryCard(
                entry = entry,
                expanded = expanded,
                onClick = { onEntryClick(entry.id) },
            )
        }
        if (isLoadingMore) {
            item(key = "log_list_loading_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

private fun consumeLogUiEffect(
    context: android.content.Context,
    dispatcher: com.example.zhttaskflow.base.ext.TaskFlowSnackbarDispatcher,
    effect: LogUiEffect,
) {
    when (effect) {
        is LogUiEffect.ShowSnackbar -> {
            val outcome = when (effect.type) {
                SnackbarType.Success -> "success"
                SnackbarType.Error -> "failure"
                SnackbarType.Normal -> "info"
            }
            logUiOutcome(
                pageId = LOG_PAGE_ID,
                actionId = "log_snackbar",
                outcome = outcome,
                params = mapOf("message" to effect.message),
            )
            showSnackbar(
                dispatcher = dispatcher,
                message = effect.message,
                type = effect.type,
            )
        }
        is LogUiEffect.ShareLogExport -> {
            logUiOutcome(
                pageId = LOG_PAGE_ID,
                actionId = "log_export_share",
                outcome = "success",
                params = mapOf("path" to effect.exportFile.absolutePath),
            )
            val authority = "${context.packageName}.observability.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, effect.exportFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(shareIntent, effect.chooserTitle),
            )
        }
    }
}
