package com.example.zhttaskflow.feature.article.presentation

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.zhttaskflow.base.extension.collectUiStateWithLifecycle
import com.example.zhttaskflow.base.ui.StateBox
import com.example.zhttaskflow.base.ui.TaskFlowScaffold
import com.example.zhttaskflow.feature.article.R
import com.example.zhttaskflow.feature.article.domain.Article
import com.example.zhttaskflow.nav.LocalTaskFlowNavigator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 资讯列表主页面：订阅 [ArticleViewModel] 状态，分发 [ArticleUiEvent]。
 */
@Composable
fun ArticleListScreen(
    viewModel: ArticleViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectUiStateWithLifecycle()
    val context = LocalContext.current
    val navigator = LocalTaskFlowNavigator.current

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ArticleUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ArticleUiEffect.NavigateToDetail -> {
                    navigator.navigate(effect.url)
                }
            }
        }
    }

    TaskFlowScaffold(
        modifier = modifier,
        title = stringResource(id = R.string.article_str_list_title),
    ) { innerPadding ->
        ArticleListContent(
            uiState = uiState,
            contentPadding = innerPadding,
            onEvent = viewModel::onEvent,
        )
    }
}

@Composable
private fun ArticleListContent(
    uiState: ArticleUiState,
    contentPadding: PaddingValues,
    onEvent: (ArticleUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    StateBox(
        uiState = uiState,
        onRetry = { onEvent(ArticleUiEvent.Refresh) },
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize(),
        emptyMessage = stringResource(id = R.string.article_str_empty_list),
        loading = { loadingModifier ->
            ArticleListSkeleton(modifier = loadingModifier)
        },
    ) { data ->
        ArticleSuccessList(
            state = data,
            onEvent = onEvent,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ArticleSuccessList(
    state: ArticleListData,
    onEvent: (ArticleUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, state.hasMore, state.isLoadingMore, state.isLoadMoreError) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to info.totalItemsCount
        }.collect { (lastVisible, total) ->
            if (
                state.hasMore &&
                !state.isLoadingMore &&
                !state.isLoadMoreError &&
                total > 0 &&
                lastVisible >= total - 2
            ) {
                onEvent(ArticleUiEvent.LoadMore)
            }
        }
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onEvent(ArticleUiEvent.Refresh) },
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(
                items = state.articles,
                key = { it.id },
            ) { article ->
                ArticleListItem(
                    article = article,
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
            item(key = "load_more_footer") {
                ArticleLoadMoreFooter(
                    isLoadingMore = state.isLoadingMore,
                    isLoadMoreError = state.isLoadMoreError,
                    hasMore = state.hasMore,
                    onRetryLoadMore = { onEvent(ArticleUiEvent.LoadMore) },
                )
            }
        }
    }
}

@Composable
private fun ArticleListItem(
    article: Article,
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
            modifier = Modifier.padding(16.dp),
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

@Composable
private fun ArticleLoadMoreFooter(
    isLoadingMore: Boolean,
    isLoadMoreError: Boolean,
    hasMore: Boolean,
    onRetryLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !hasMore -> {
                Text(
                    text = stringResource(id = R.string.article_str_no_more),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            isLoadingMore -> {
                CircularProgressIndicator()
            }
            isLoadMoreError -> {
                TextButton(onClick = onRetryLoadMore) {
                    Text(text = stringResource(id = R.string.article_str_load_more_retry))
                }
            }
            else -> {
                Text(
                    text = stringResource(id = R.string.article_str_load_more_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ArticleListSkeleton(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp),
    ) {
        items(6) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            ) {}
        }
    }
}

private fun formatPublishedAt(epochMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}
