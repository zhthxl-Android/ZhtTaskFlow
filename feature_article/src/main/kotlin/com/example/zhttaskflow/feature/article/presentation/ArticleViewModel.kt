package com.example.zhttaskflow.feature.article.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.mvi.BaseViewModel
import com.example.zhttaskflow.base.mvi.getDataOrNull
import com.example.zhttaskflow.core.util.isNotNullOrBlank
import com.example.zhttaskflow.feature.article.domain.ArticlePage
import com.example.zhttaskflow.feature.article.domain.ArticlePagingDefaults
import com.example.zhttaskflow.feature.article.domain.usecase.GetArticlePageUseCase
import com.example.zhttaskflow.feature.article.domain.usecase.RefreshArticlePageUseCase
import com.example.zhttaskflow.nav.route.TaskFlowArticleNavRoutes

/**
 * 资讯列表 ViewModel：MVI 单向数据流，通过领域用例调度分页与 UI 状态。
 *
 * **调用方式**：UI 通过 [onEvent] 投递 [ArticleUiEvent]；订阅 [uiState] 渲染，订阅 [uiEffect] 处理 Snackbar/导航。
 *
 * **线程约束**：数据加载在 [launchTask] 内执行（用例 → 仓库 IO）；本类不直接访问 Repository 与网络/数据库 SDK。
 */
class ArticleViewModel(
    private val getArticlePageUseCase: GetArticlePageUseCase,
    private val refreshArticlePageUseCase: RefreshArticlePageUseCase,
) : BaseViewModel<ArticleUiState, ArticleUiEvent, ArticleUiEffect>(BaseUiState.Loading) {

    private val logTag = "ArticleViewModel"
    private val pageSize = ArticlePagingDefaults.DEFAULT_PAGE_SIZE

    // region 初始化入口

    init {
        loadFirstPage()
    }

    // endregion

    // region 事件分发

    override fun handleEvent(event: ArticleUiEvent) {
        when (event) {
            ArticleUiEvent.Refresh -> refresh()
            ArticleUiEvent.LoadMore -> loadMore()
            is ArticleUiEvent.ArticleClicked -> navigateToDetail(event.articleId, event.detailUrl)
        }
    }

    // endregion

    // region 首页加载

    private fun loadFirstPage() {
        setState { BaseUiState.Loading }
        launchTask(
            tag = logTag,
            scene = "loadFirstPage",
            userMessageFallback = "加载失败，请稍后重试",
            onError = { _, message ->
                setState {
                    BaseUiState.Error(message)
                }
                sendEffect(
                    ArticleUiEffect.ShowSnackbar(message = message, type = SnackbarType.Error),
                )
            },
        ) {
            val page = getArticlePageUseCase(
                page = ArticlePagingDefaults.FIRST_PAGE,
                pageSize = pageSize,
            )
            applyPageResult(page = page, append = false, isRefresh = false)
        }
    }

    // endregion

    // region 下拉刷新

    private fun refresh() {
        val current = currentState
        if (current is BaseUiState.Success) {
            setState {
                BaseUiState.Success(current.data.withRefreshing())
            }
        }
        launchTask(
            tag = logTag,
            scene = "refresh",
            userMessageFallback = "刷新失败，请稍后重试",
            onError = { _, message ->
                when (val state = currentState) {
                    is BaseUiState.Success -> {
                        setState {
                            BaseUiState.Success(
                                state.data.copy(isRefreshing = false),
                            )
                        }
                    }
                    else -> {
                        setState {
                            BaseUiState.Error(message)
                        }
                    }
                }
                sendEffect(
                    ArticleUiEffect.ShowSnackbar(message = message, type = SnackbarType.Error),
                )
            },
        ) {
            val page = refreshArticlePageUseCase(
                page = ArticlePagingDefaults.FIRST_PAGE,
                pageSize = pageSize,
            )
            applyPageResult(page = page, append = false, isRefresh = true)
        }
    }

    // endregion

    // region 加载更多

    private fun loadMore() {
        val state = currentState as? BaseUiState.Success ?: return
        val data = state.data
        if (!data.hasMore || data.isLoadingMore) {
            return
        }
        setState {
            BaseUiState.Success(data.withLoadingMore())
        }
        val nextPage = data.currentPage + 1
        launchTask(
            tag = logTag,
            scene = "loadMore",
            userMessageFallback = "加载更多失败，请稍后重试",
            onError = { _, message ->
                setState {
                    BaseUiState.Success(data.withLoadMoreError())
                }
                sendEffect(
                    ArticleUiEffect.ShowSnackbar(message = message, type = SnackbarType.Error),
                )
            },
        ) {
            val page = getArticlePageUseCase(page = nextPage, pageSize = pageSize)
            val merged = data.articles + page.articles
            setState {
                BaseUiState.Success(
                    ArticleListData(
                        articles = merged,
                        currentPage = nextPage,
                        hasMore = page.hasMore,
                        isRefreshing = false,
                        isLoadingMore = false,
                        isLoadMoreError = false,
                    ),
                )
            }
        }
    }

    // endregion

    // region 结果统一处理

    private fun applyPageResult(
        page: ArticlePage,
        append: Boolean,
        isRefresh: Boolean,
    ) {
        if (page.articles.isEmpty() && !append) {
            setState { BaseUiState.Empty }
            return
        }
        val previous = currentState.getDataOrNull()
        val articles = if (append && previous != null) {
            previous.articles + page.articles
        } else {
            page.articles
        }
        setState {
            BaseUiState.Success(
                ArticleListData(
                    articles = articles,
                    currentPage = page.page,
                    hasMore = page.hasMore,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isLoadMoreError = false,
                ),
            )
        }
        if (isRefresh && articles.isNotEmpty()) {
            sendEffect(
                ArticleUiEffect.ShowSnackbar(
                    message = "刷新成功",
                    type = SnackbarType.Success,
                ),
            )
        }
    }

    // endregion

    // region 导航处理

    private fun navigateToDetail(articleId: String, detailUrl: String) {
        if (!articleId.isNotNullOrBlank() || !detailUrl.isNotNullOrBlank()) {
            sendEffect(
                ArticleUiEffect.ShowSnackbar(
                    message = "无法打开详情",
                    type = SnackbarType.Error,
                ),
            )
            return
        }
        sendEffect(
            ArticleUiEffect.NavigateToDetail(
                url = TaskFlowArticleNavRoutes.detailPath(
                    articleId = articleId,
                    detailUrl = detailUrl,
                ),
            ),
        )
    }

    // endregion
}

private fun ArticleListData.withRefreshing(): ArticleListData = copy(
    isRefreshing = true,
    isLoadingMore = false,
    isLoadMoreError = false,
)

private fun ArticleListData.withLoadingMore(): ArticleListData = copy(
    isLoadingMore = true,
    isLoadMoreError = false,
)

private fun ArticleListData.withLoadMoreError(): ArticleListData = copy(
    isLoadingMore = false,
    isLoadMoreError = true,
)

/**
 * [ArticleViewModel] 手动注入工厂（无 Hilt）。
 */
class ArticleViewModelFactory(
    private val getArticlePageUseCase: GetArticlePageUseCase,
    private val refreshArticlePageUseCase: RefreshArticlePageUseCase,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticleViewModel::class.java)) {
            return ArticleViewModel(
                getArticlePageUseCase = getArticlePageUseCase,
                refreshArticlePageUseCase = refreshArticlePageUseCase,
            ) as T
        }
        throw IllegalArgumentException("未知 ViewModel: ${modelClass.name}")
    }
}
