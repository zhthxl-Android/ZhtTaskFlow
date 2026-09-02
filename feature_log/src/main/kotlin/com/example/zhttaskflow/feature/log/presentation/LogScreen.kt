package com.example.zhttaskflow.feature.log.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.extension.collectUiStateWithLifecycle
import com.example.zhttaskflow.base.ui.TaskFlowListScaffold
import com.example.zhttaskflow.base.ui.extension.PageLifecycleLog
import com.example.zhttaskflow.feature.log.R

private const val LOG_VIEWER_PAGE_ID: String = "LogViewer"

/**
 * 日志查看 Tab 页骨架；列表与筛选在后续任务实现。
 */
@Composable
internal fun LogScreen(
    viewModel: LogViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectUiStateWithLifecycle()

    PageLifecycleLog(
        pageName = LOG_VIEWER_PAGE_ID,
        pageArgs = when (uiState) {
            is BaseUiState.Empty -> "empty"
            is BaseUiState.Loading -> "loading"
            is BaseUiState.Error -> "error"
            is BaseUiState.Success -> "ready"
        },
    )

    TaskFlowListScaffold(
        modifier = modifier,
        title = stringResource(id = R.string.log_str_viewer_title),
        interceptTabRootBackToDesktop = false,
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = stringResource(id = R.string.log_str_viewer_placeholder))
        }
    }
}
