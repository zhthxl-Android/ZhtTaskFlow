package com.example.zhttaskflow.feature.log.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.ext.rememberTaskFlowDialogController
import com.example.zhttaskflow.base.ext.rememberTaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ext.showSnackbar
import com.example.zhttaskflow.base.observability.TaskFlowDeveloperObservability
import com.example.zhttaskflow.base.ui.extension.PageLifecycleLog
import com.example.zhttaskflow.base.ui.TaskFlowListScaffold
import com.example.zhttaskflow.base.ui.TaskFlowUiConstants
import com.example.zhttaskflow.core.debug.TaskFlowDeveloperTools
import com.example.zhttaskflow.core.observability.TaskFlowLocalLogStore
import com.example.zhttaskflow.feature.log.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

/**
 * Debug 安装包专用深度调试面板：可观测切换、日志注入、性能/崩溃模拟、离线横幅模拟。
 *
 * Release 构建不会展示入口；本页不替代日志 Tab 的查看/导出/清空能力。
 */
@Composable
internal fun DebugSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dialogController = rememberTaskFlowDialogController()
    val snackbarDispatcher = rememberTaskFlowSnackbarDispatcher()
    val scope = rememberCoroutineScope()
    var observabilityBackend by remember {
        mutableStateOf(TaskFlowDeveloperTools.observabilityBackend)
    }
    var injectChannel by remember { mutableStateOf(InjectChannel.ANALYTICS) }
    var injectCountText by remember { mutableStateOf("50") }
    var simulateOffline by remember {
        mutableStateOf(TaskFlowDeveloperTools.simulateNetworkOffline)
    }

    val confirmText = stringResource(id = R.string.log_str_confirm)
    val dismissText = stringResource(id = R.string.log_str_cancel)
    val injectDoneTemplate = stringResource(id = R.string.log_str_debug_inject_done)
    val slowConfirmMessage = stringResource(id = R.string.log_str_debug_simulate_slow_confirm)
    val doneGenericMessage = stringResource(id = R.string.log_str_debug_done_generic)
    val slowTitle = stringResource(id = R.string.log_str_debug_simulate_slow)

    PageLifecycleLog(pageName = DEBUG_SETTINGS_PAGE_ID)

    TaskFlowListScaffold(
        modifier = modifier,
        title = stringResource(id = R.string.log_str_debug_title),
        interceptTabRootBackToDesktop = false,
        actions = {
            Button(onClick = onBack) {
                Text(text = stringResource(id = R.string.log_str_debug_back))
            }
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TaskFlowUiConstants.PageHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.ListVerticalSpacing),
        ) {
            SectionTitle(text = stringResource(id = R.string.log_str_debug_observability_section))
            ObservabilityBackendRow(
                selected = observabilityBackend,
                onSelected = { backend ->
                    observabilityBackend = backend
                    TaskFlowDeveloperTools.setObservabilityBackend(backend)
                    showSnackbar(
                        dispatcher = snackbarDispatcher,
                        message = doneGenericMessage,
                        type = SnackbarType.Normal,
                    )
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = TaskFlowUiConstants.ListVerticalSpacing))

            SectionTitle(text = stringResource(id = R.string.log_str_debug_inject_section))
            InjectChannelRow(
                selected = injectChannel,
                onSelected = { injectChannel = it },
            )
            OutlinedTextField(
                value = injectCountText,
                onValueChange = { injectCountText = it.filter { ch -> ch.isDigit() }.take(4) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(id = R.string.log_str_debug_inject_count_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
            )
            Button(
                onClick = {
                    val count = injectCountText.toIntOrNull()?.coerceIn(1, 5_000) ?: 50
                    val channel = when (injectChannel) {
                        InjectChannel.ANALYTICS ->
                            TaskFlowLocalLogStore.ObservabilityChannel.ANALYTICS
                        InjectChannel.PERFORMANCE ->
                            TaskFlowLocalLogStore.ObservabilityChannel.PERFORMANCE
                        InjectChannel.CRASH ->
                            TaskFlowLocalLogStore.ObservabilityChannel.CRASH
                    }
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            TaskFlowDeveloperTools.injectTestLogs(channel = channel, count = count)
                        }
                        showSnackbar(
                        dispatcher = snackbarDispatcher,
                        message = String.format(injectDoneTemplate, count),
                        type = SnackbarType.Success,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.log_str_debug_inject_button))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = TaskFlowUiConstants.ListVerticalSpacing))

            SectionTitle(text = stringResource(id = R.string.log_str_debug_perf_section))
            Button(
                onClick = {
                    TaskFlowDeveloperObservability.simulateAnrReport()
                    showSnackbar(
                        dispatcher = snackbarDispatcher,
                        message = doneGenericMessage,
                        type = SnackbarType.Normal,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.log_str_debug_simulate_anr))
            }
            Button(
                onClick = {
                    TaskFlowDeveloperObservability.simulateNonFatalCrash()
                    showSnackbar(
                        dispatcher = snackbarDispatcher,
                        message = doneGenericMessage,
                        type = SnackbarType.Normal,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.log_str_debug_simulate_crash))
            }
            Button(
                onClick = {
                    dialogController.showConfirmDialog(
                        title = slowTitle,
                        message = slowConfirmMessage,
                        confirmText = confirmText,
                        dismissText = dismissText,
                        onConfirm = {
                            TaskFlowDeveloperObservability.simulateSlowFunction(blockMainThreadMs = 2_000L)
                            showSnackbar(
                                dispatcher = snackbarDispatcher,
                                message = doneGenericMessage,
                                type = SnackbarType.Normal,
                            )
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(id = R.string.log_str_debug_simulate_slow))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = TaskFlowUiConstants.ListVerticalSpacing))

            SectionTitle(text = stringResource(id = R.string.log_str_debug_network_section))
            RowSwitch(
                label = stringResource(id = R.string.log_str_debug_network_offline),
                checked = simulateOffline,
                onCheckedChange = { enabled ->
                    simulateOffline = enabled
                    TaskFlowDeveloperTools.setSimulateNetworkOffline(enabled)
                },
            )
            Text(
                text = stringResource(id = R.string.log_str_debug_network_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(TaskFlowUiConstants.ListVerticalSpacing))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = TaskFlowUiConstants.ListVerticalSpacing),
    )
}

@Composable
private fun ObservabilityBackendRow(
    selected: TaskFlowDeveloperTools.ObservabilityBackend,
    onSelected: (TaskFlowDeveloperTools.ObservabilityBackend) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.ListVerticalSpacing)) {
        FilterChip(
            selected = selected == TaskFlowDeveloperTools.ObservabilityBackend.APP_SHELL_DEFAULT,
            onClick = { onSelected(TaskFlowDeveloperTools.ObservabilityBackend.APP_SHELL_DEFAULT) },
            label = { Text(text = stringResource(id = R.string.log_str_debug_observability_shell)) },
        )
        FilterChip(
            selected = selected == TaskFlowDeveloperTools.ObservabilityBackend.RELEASE_LOCAL,
            onClick = { onSelected(TaskFlowDeveloperTools.ObservabilityBackend.RELEASE_LOCAL) },
            label = { Text(text = stringResource(id = R.string.log_str_debug_observability_release_local)) },
        )
    }
}

private enum class InjectChannel {
    ANALYTICS,
    PERFORMANCE,
    CRASH,
}

@Composable
private fun InjectChannelRow(
    selected: InjectChannel,
    onSelected: (InjectChannel) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.ListVerticalSpacing)) {
        FilterChip(
            selected = selected == InjectChannel.ANALYTICS,
            onClick = { onSelected(InjectChannel.ANALYTICS) },
            label = { Text(text = stringResource(id = R.string.log_str_filter_analytics)) },
        )
        FilterChip(
            selected = selected == InjectChannel.PERFORMANCE,
            onClick = { onSelected(InjectChannel.PERFORMANCE) },
            label = { Text(text = stringResource(id = R.string.log_str_filter_performance)) },
        )
        FilterChip(
            selected = selected == InjectChannel.CRASH,
            onClick = { onSelected(InjectChannel.CRASH) },
            label = { Text(text = stringResource(id = R.string.log_str_filter_crash)) },
        )
    }
}

@Composable
private fun RowSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 深度调试面板 pageId（与 [PageLifecycleLog] / 交互埋点命名一致）。 */
internal const val DEBUG_SETTINGS_PAGE_ID: String = "DebugSettings"
