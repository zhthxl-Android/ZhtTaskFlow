package com.example.zhttaskflow.feature.log.presentation

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.zhttaskflow.base.ext.SnackbarType
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
            "count=${data.entries.size};filter=${data.filter.name}"
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
                    viewModel.onEvent(LogUiEvent.ExportRequested)
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
                    listContentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
    listContentPadding: PaddingValues,
    onEntryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        BaseEmptyScreen(
            message = stringResource(id = R.string.log_str_empty),
            modifier = modifier.fillMaxSize(),
        )
        return
    }
    LazyColumn(
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
    }
}

@Composable
private fun LogEntryCard(
    entry: LogEntryUi,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(id = R.string.log_str_entry_time, entry.timestampText),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(id = R.string.log_str_entry_type, entry.typeLabel),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(id = R.string.log_str_entry_page_id, entry.pageId),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(id = R.string.log_str_entry_action_id, entry.actionId),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = stringResource(id = R.string.log_str_entry_summary, entry.summary),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
            )
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(id = R.string.log_str_entry_detail),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = entry.detailText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                    )
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
