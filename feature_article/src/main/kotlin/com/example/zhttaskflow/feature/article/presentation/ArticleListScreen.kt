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
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.ext.SnackbarDispatcher
import com.example.zhttaskflow.base.ext.rememberSnackbarDispatcher
import com.example.zhttaskflow.base.ext.showSnackbar
import com.example.zhttaskflow.base.extension.collectUiStateWithLifecycle
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.ui.ListPaginationState
import com.example.zhttaskflow.base.ui.ListScaffold
import com.example.zhttaskflow.base.ui.PaginatedListPayload
import com.example.zhttaskflow.base.ui.rememberStateBoxContentPadding
import com.example.zhttaskflow.base.ui.StatePaginatedListContent
import com.example.zhttaskflow.base.ui.UiConstants
import com.example.zhttaskflow.base.ui.extension.PageLifecycleLog
import com.example.zhttaskflow.base.ui.extension.listItemClickWithLog
import com.example.zhttaskflow.base.ui.extension.logUiInteraction
import com.example.zhttaskflow.base.ui.extension.logUiOutcome
import com.example.zhttaskflow.base.ui.rememberListLazyContentPadding
import com.example.zhttaskflow.feature.article.R
import com.example.zhttaskflow.feature.article.domain.Article
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val ARTICLE_LIST_PAGE_ID: String = "ArticleList"

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

    ListScaffold(
        modifier = modifier,
        collapsibleTopBarOnScroll = true,
        // 启动 Tab 根页：系统返回退桌面（与资讯列表 Tab 行为一致）
        interceptTabRootBackToDesktop = true,
    ) { scaffoldContentPadding ->
        val snackbarDispatcher = rememberSnackbarDispatcher()
        LaunchedEffect(viewModel, snackbarDispatcher) {
            viewModel.uiEffect.collect { effect ->
                consumeArticleListUiEffect(
                    dispatcher = snackbarDispatcher,
                    effect = effect,
                )
            }
        }
        val listContentPadding = rememberListLazyContentPadding(
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
    StatePaginatedListContent(
        uiState = uiState.toPaginatedUiState(),
        onRetry = {
            logUiInteraction(
                action = "click",
                identifier = "article_list_retry",
                pageId = ARTICLE_LIST_PAGE_ID,
            )
            onEvent(ArticleUiEvent.Refresh)
        },
        onRefresh = {
            logUiInteraction(
                action = "pullRefresh",
                identifier = "article_list",
                pageId = ARTICLE_LIST_PAGE_ID,
            )
            onEvent(ArticleUiEvent.Refresh)
        },
        onLoadMore = {
            logUiInteraction(
                action = "loadMore",
                identifier = "article_list_load_more",
                pageId = ARTICLE_LIST_PAGE_ID,
            )
            onEvent(ArticleUiEvent.LoadMore)
        },
        onRetryLoadMore = {
            logUiInteraction(
                action = "click",
                identifier = "article_list_load_more_retry",
                pageId = ARTICLE_LIST_PAGE_ID,
            )
            onEvent(ArticleUiEvent.LoadMore)
        },
        listContentPadding = listContentPadding,
        modifier = modifier.fillMaxSize(),
        contentPadding = rememberStateBoxContentPadding(),
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

private fun ArticleUiState.toPaginatedUiState(): BaseUiState<PaginatedListPayload<Article>> =
    when (this) {
        BaseUiState.Loading -> BaseUiState.Loading
        BaseUiState.Empty -> BaseUiState.Empty
        is BaseUiState.Error -> BaseUiState.Error(message = message)
        is BaseUiState.Success -> BaseUiState.Success(
            data = PaginatedListPayload(
                items = data.articles,
                pagination = ListPaginationState(
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
                pageId = ARTICLE_LIST_PAGE_ID,
                params = mapOf("articleId" to article.id),
                onClick = onClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(UiConstants.PageHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(UiConstants.ListItemCardInnerSpacing),
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

/**
 * Screen 层 Collector：仅处理 [com.example.zhttaskflow.base.ext.PresentationUiEffect]。
 * 导航类 Effect 由 [com.example.zhttaskflow.feature.article.navigation.ArticleListRouteHost] 消费，见 [com.example.zhttaskflow.base.ext.UiEffectConsumption]。
 */
private fun consumeArticleListUiEffect(
    dispatcher: SnackbarDispatcher,
    effect: ArticleUiEffect,
) {
    when (effect) {
        is ArticleUiEffect.ShowSnackbar -> {
            val outcome = when (effect.type) {
                SnackbarType.Success -> "success"
                SnackbarType.Error -> "failure"
                SnackbarType.Normal -> "info"
            }
            logUiOutcome(
                pageId = ARTICLE_LIST_PAGE_ID,
                actionId = "article_list_snackbar",
                outcome = outcome,
                params = mapOf("message" to effect.message),
            )
            showSnackbar(
                dispatcher = dispatcher,
                message = effect.message,
                type = effect.type,
            )
        }
        is ArticleUiEffect.NavigateToDetail -> {
            // NavigationUiEffect：由 ArticleListRouteHost 消费
        }
    }
}
