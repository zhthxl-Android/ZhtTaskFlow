package com.example.zhttaskflow.feature.home.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.example.zhttaskflow.base.ui.icon.TaskFlowIcons
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.zhttaskflow.base.extension.collectUiStateWithLifecycle
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.ext.TaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ext.rememberTaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ext.showSnackbar
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.ui.StateBox
import com.example.zhttaskflow.base.ui.TaskFlowListScaffold
import com.example.zhttaskflow.base.ui.TaskFlowUiConstants
import com.example.zhttaskflow.base.ui.extension.PageLifecycleLog
import com.example.zhttaskflow.base.ui.extension.logUiInteraction
import com.example.zhttaskflow.base.ui.rememberTaskFlowStateBoxContentPadding
import com.example.zhttaskflow.feature.home.domain.HomeEntranceIds

private const val HOME_PAGE_ID: String = "Home"

/**
 * 首页纯 UI：订阅状态并分发事件；消费全部页面内 UI 类 [HomeUiEffect]（导航类由路由宿主处理）。
 */
@Composable
internal fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onTabRootBackPress: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectUiStateWithLifecycle()
    val lifecycleArgs = when (val state = uiState) {
        is BaseUiState.Success -> "entrances=${state.data.entrances.size}"
        is BaseUiState.Loading -> "loading"
        is BaseUiState.Error -> "error"
        is BaseUiState.Empty -> "empty"
    }

    PageLifecycleLog(
        pageName = "Home",
        pageArgs = lifecycleArgs,
    )

    TaskFlowListScaffold(
        modifier = modifier,
        interceptTabRootBackToDesktop = true,
        onTabRootBackPress = onTabRootBackPress,
    ) { _ ->
        val snackbarDispatcher = rememberTaskFlowSnackbarDispatcher()
        LaunchedEffect(viewModel, snackbarDispatcher) {
            viewModel.uiEffect.collect { effect ->
                consumeHomeUiEffect(
                    dispatcher = snackbarDispatcher,
                    effect = effect,
                )
            }
        }
        val pagePadding = rememberTaskFlowStateBoxContentPadding()
        StateBox(
            uiState = uiState,
            onRetry = {
                logUiInteraction(
                    action = "click",
                    identifier = "home_retry",
                    pageId = HOME_PAGE_ID,
                )
                viewModel.onEvent(HomeUiEvent.Retry)
            },
            contentPadding = pagePadding,
            modifier = Modifier.fillMaxSize(),
        ) { data ->
            HomeEntranceList(
                entrances = data.entrances,
                onEntranceClick = { entranceId ->
                    viewModel.onEvent(HomeUiEvent.EntranceClicked(entranceId = entranceId))
                },
            )
        }
    }
}

@Composable
private fun HomeEntranceList(
    entrances: List<HomeEntrance>,
    onEntranceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = TaskFlowUiConstants.PageHorizontalPadding +
                    TaskFlowUiConstants.ListVerticalSpacing,
                vertical = TaskFlowUiConstants.PageHorizontalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(
            TaskFlowUiConstants.PageHorizontalPadding,
            Alignment.CenterVertically,
        ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        entrances.forEachIndexed { index, entrance ->
            HomeEntryCard(
                title = entrance.title,
                description = entrance.description,
                icon = entranceIcon(entrance.id),
                onClick = {
                    logUiInteraction(
                        action = "click",
                        identifier = "home_entrance_card",
                        pageId = HOME_PAGE_ID,
                        params = mapOf(
                            "entranceId" to entrance.id,
                            "index" to index.toString(),
                        ),
                    )
                    onEntranceClick(entrance.id)
                },
            )
        }
    }
}

@Composable
private fun HomeEntryCard(
    title: String,
    description: String,
    icon: ImageVector,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    TaskFlowUiConstants.PageHorizontalPadding +
                        TaskFlowUiConstants.ListVerticalSpacing / 2,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.PageHorizontalPadding),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(
                    TaskFlowUiConstants.DialogActionHeight - TaskFlowUiConstants.ListVerticalSpacing,
                ),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.ListVerticalSpacing / 2),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun entranceIcon(entranceId: String): ImageVector {
    return when (entranceId) {
        HomeEntranceIds.TASK -> TaskFlowIcons.HomeEntrance.Task
        HomeEntranceIds.ARTICLE -> TaskFlowIcons.HomeEntrance.Article
        else -> TaskFlowIcons.HomeEntrance.Task
    }
}

/**
 * Screen 层 Collector：仅处理 [com.example.zhttaskflow.base.ext.TaskFlowPresentationUiEffect]。
 * 导航类 Effect 由 [com.example.zhttaskflow.feature.home.navigation.HomeRouteHost] 消费，见 [com.example.zhttaskflow.base.ext.TaskFlowUiEffectConsumption]。
 */
private fun consumeHomeUiEffect(
    dispatcher: TaskFlowSnackbarDispatcher,
    effect: HomeUiEffect,
) {
    when (effect) {
        is HomeUiEffect.ShowSnackbar -> {
            val outcomeAction = when (effect.type) {
                SnackbarType.Success -> "success"
                SnackbarType.Error -> "failure"
                SnackbarType.Normal -> "info"
            }
            logUiInteraction(
                action = outcomeAction,
                identifier = "home_snackbar",
                pageId = HOME_PAGE_ID,
                params = mapOf("message" to effect.message),
            )
            showSnackbar(
                dispatcher = dispatcher,
                message = effect.message,
                type = effect.type,
            )
        }
        is HomeUiEffect.NavigateToRoute -> {
            // TaskFlowNavigationUiEffect：由 HomeRouteHost 消费
        }
    }
}
