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

//埋点 ID，所有这个页面的点击、曝光、结果埋点都带这个 ID，统一标识页面来源，方便埋点统计
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
    //根据当前页面状态，生成不同的埋点参数
    val lifecycleArgs = when (val state = uiState) {
        is BaseUiState.Success -> "page=${state.data.currentPage} count=${state.data.articles.size}"
        is BaseUiState.Loading -> "loading"
        is BaseUiState.Error -> "error"
        is BaseUiState.Empty -> "empty"
    }
    //传入统计方法
    PageLifecycleLog(
        pageName = "ArticleList",
        pageArgs = lifecycleArgs,
    )
    //页面脚手架：顶栏、折叠、返回拦截
    ListScaffold(
        modifier = modifier,
        collapsibleTopBarOnScroll = true,//开启滚动折叠顶栏，列表下滑顶栏收起，上滑展开
        // 启动 Tab 根页：系统返回退桌面（与资讯列表 Tab 行为一致）
        interceptTabRootBackToDesktop = true,
    ) { scaffoldContentPadding ->
        //Snackbar 的分发管理器，用来控制 Snackbar 的显示、隐藏
        val snackbarDispatcher = rememberSnackbarDispatcher()
        LaunchedEffect(viewModel, snackbarDispatcher) {
            viewModel.uiEffect.collect { effect ->
                consumeArticleListUiEffect(
                    dispatcher = snackbarDispatcher,
                    effect = effect,
                )
            }
        }
        //因为开启了折叠顶栏，后面必须用 `rememberListLazyContentPadding` 把顶部清零，否则折叠后会留白
        //滚动友好、带左右边距、保留底部避让内边距
        val listContentPadding = rememberListLazyContentPadding(
            scaffoldPadding = scaffoldContentPadding,
        )
        //把状态、内边距、事件回调传给内容层，由内容层对接通用列表组件
        ArticleListContent(
            uiState = uiState,
            listContentPadding = listContentPadding,
            onEvent = viewModel::onEvent,//UI 事件统一回传给 ViewModel 处理
        )
    }
}

/**
 * 把业务状态转换成通用列表组件能识别的格式，
 * 业务组装：状态转换、事件包装、埋点
 * */
@Composable
private fun ArticleListContent(
    uiState: ArticleUiState,
    listContentPadding: PaddingValues,
    onEvent: (ArticleUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    //调用一站式分页列表组件：四态+刷新+分页+骨架
    StatePaginatedListContent(
        //把业务状态 ArticleUiState 转换成通用分页列表状态
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
        listContentPadding = listContentPadding,//列表专属内边距，给内部的 LazyColumn 使用
        modifier = modifier.fillMaxSize(),
        contentPadding = rememberStateBoxContentPadding(),//`StateBox` 自身的内容内边距，控制 Loading/Empty/Error 页面的左右留白
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

/**
 * 把业务的 `ArticleUiState` 映射成通用列表组件需要的 `BaseUiState<PaginatedListPayload<Article>>`
 * 这个扩展函数将文章的UI状态转换为带有分页列表载荷的UI状态
 *
 * @return 返回转换后的 BaseUiState<PaginatedListPayload<Article>> 类型
 */
private fun ArticleUiState.toPaginatedUiState(): BaseUiState<PaginatedListPayload<Article>> =
    when (this) {
        // 当状态为加载中时，直接返回加载状态
        BaseUiState.Loading -> BaseUiState.Loading
        // 当状态为空时，直接返回空状态
        BaseUiState.Empty -> BaseUiState.Empty
        // 当状态为错误时，返回带有错误信息的错误状态
        is BaseUiState.Error -> BaseUiState.Error(message = message)
        // 当状态为成功时，构建带有分页信息的成功状态
        is BaseUiState.Success -> BaseUiState.Success(
            data = PaginatedListPayload(
                // 提取文章列表作为数据项
                items = data.articles,
                // 构建分页状态信息
                pagination = ListPaginationState(
                    isRefreshing = data.isRefreshing,    // 是否正在刷新
                    isLoadingMore = data.isLoadingMore,  // 是否正在加载更多
                    isLoadMoreError = data.isLoadMoreError, // 是否加载更多出错
                    hasMore = data.hasMore,             // 是否还有更多数据
                ),
            ),
        )
    }

/**
 * 单条资讯的卡片 UI，纯展示组件，不持有业务逻辑。
 * */
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
            .listItemClickWithLog(//添加点击上报
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
 * 处理 ViewModel 发出的一次性 UI 效果
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
            //上报结果埋点
            logUiOutcome(
                pageId = ARTICLE_LIST_PAGE_ID,
                actionId = "article_list_snackbar",
                outcome = outcome,
                params = mapOf("message" to effect.message),
            )
            //通过分发器弹出提示
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
