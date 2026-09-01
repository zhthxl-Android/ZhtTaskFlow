package com.example.zhttaskflow.feature.article.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.ext.TaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ext.rememberTaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ext.showSnackbar
import com.example.zhttaskflow.base.extension.collectUiStateWithLifecycle
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.ui.TaskFlowListPaginationState
import com.example.zhttaskflow.base.ui.TaskFlowListScaffold
import com.example.zhttaskflow.base.ui.TaskFlowPaginatedListPayload
import com.example.zhttaskflow.base.ui.TaskFlowStatePaginatedListContent
import com.example.zhttaskflow.base.ui.TaskFlowUiConstants
import com.example.zhttaskflow.base.ui.extension.PageLifecycleLog
import com.example.zhttaskflow.base.ui.extension.listItemClickWithLog
import com.example.zhttaskflow.base.ui.extension.logUiInteraction
import com.example.zhttaskflow.base.ui.rememberTaskFlowListLazyContentPadding
import com.example.zhttaskflow.feature.article.R
import com.example.zhttaskflow.feature.article.domain.Article
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 资讯列表主页面：订阅状态并分发事件；消费全部页面内 UI 类 [ArticleUiEffect]（导航类由路由宿主处理）。
 */
@Composable
fun ArticleListScreen(
    viewModel: ArticleViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectUiStateWithLifecycle()
    val lifecycleArgs = when (val state = uiState) {
        is BaseUiState.Success -> "page=${state.data.currentPage} count=${state.data.articles.size}"
        is BaseUiState.Loading -> "loading"
        is BaseUiState.Error -> "error"
        is BaseUiState.Empty -> "empty"
    }

    PageLifecycleLog(
        pageName = "ArticleList",
        pageArgs = lifecycleArgs,
    )

    TaskFlowListScaffold(
        modifier = modifier,
        collapsibleTopBarOnScroll = true,
        interceptTabRootBackToDesktop = true,
    ) { scaffoldContentPadding ->
        val snackbarDispatcher = rememberTaskFlowSnackbarDispatcher()
        LaunchedEffect(viewModel, snackbarDispatcher) {
            viewModel.uiEffect.collect { effect ->
                consumeArticleListUiEffect(
                    dispatcher = snackbarDispatcher,
                    effect = effect,
                )
            }
        }
        val listContentPadding = rememberTaskFlowListLazyContentPadding(
            scaffoldPadding = scaffoldContentPadding,
        )
        ArticleListContent(
            uiState = uiState,
            listContentPadding = listContentPadding,
            onEvent = viewModel::onEvent,
        )
    }
}

@Composable
private fun ArticleListContent(
    uiState: ArticleUiState,
    listContentPadding: PaddingValues,
    onEvent: (ArticleUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    TaskFlowStatePaginatedListContent(
        uiState = uiState.toPaginatedUiState(),
        onRetry = {
            logUiInteraction(action = "click", identifier = "article_list_retry")
            onEvent(ArticleUiEvent.Refresh)
        },
        onRefresh = {
            logUiInteraction(action = "pullRefresh", identifier = "article_list")
            onEvent(ArticleUiEvent.Refresh)
        },
        onLoadMore = { onEvent(ArticleUiEvent.LoadMore) },
        onRetryLoadMore = {
            logUiInteraction(action = "click", identifier = "article_list_load_more_retry")
            onEvent(ArticleUiEvent.LoadMore)
        },
        listContentPadding = listContentPadding,
        modifier = modifier.fillMaxSize(),
        emptyMessage = stringResource(id = R.string.article_str_empty_list),
        key = { _, article -> article.id },
    ) { index, article ->
        ArticleListItem(
            article = article,
            index = index,
            onClick = {
                onEvent(
                    ArticleUiEvent.ArticleClicked(
                        articleId = article.id,
                        detailUrl = article.detailUrl,
                    ),
                )
            },
        )
    }
}

private fun ArticleUiState.toPaginatedUiState(): BaseUiState<TaskFlowPaginatedListPayload<Article>> =
    when (this) {
        BaseUiState.Loading -> BaseUiState.Loading
        BaseUiState.Empty -> BaseUiState.Empty
        is BaseUiState.Error -> BaseUiState.Error(message = message)
        is BaseUiState.Success -> BaseUiState.Success(
            data = TaskFlowPaginatedListPayload(
                items = data.articles,
                pagination = TaskFlowListPaginationState(
                    isRefreshing = data.isRefreshing,
                    isLoadingMore = data.isLoadingMore,
                    isLoadMoreError = data.isLoadMoreError,
                    hasMore = data.hasMore,
                ),
            ),
        )
    }

@Composable
private fun ArticleListItem(
    article: Article,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .listItemClickWithLog(
                identifier = "article_list_item",
                index = index,
                detail = "articleId=${article.id}",
                onClick = onClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(TaskFlowUiConstants.PageHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    id = R.string.article_str_item_meta,
                    article.author,
                    article.category,
                    formatPublishedAt(article.publishedAt),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = article.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private fun formatPublishedAt(epochMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}

@Suppress("DEPRECATION")
private fun consumeArticleListUiEffect(
    dispatcher: TaskFlowSnackbarDispatcher,
    effect: ArticleUiEffect,
) {
    when (effect) {
        is ArticleUiEffect.ShowSnackbar -> {
            showSnackbar(
                dispatcher = dispatcher,
                message = effect.message,
                type = effect.type,
            )
        }
        is ArticleUiEffect.ShowToast -> {
            showSnackbar(
                dispatcher = dispatcher,
                message = effect.message,
                type = SnackbarType.Normal,
            )
        }
        is ArticleUiEffect.NavigateToDetail -> {
            // 跨页面导航：由 ArticleListRouteHost 消费，Screen 不处理
        }
    }
}
