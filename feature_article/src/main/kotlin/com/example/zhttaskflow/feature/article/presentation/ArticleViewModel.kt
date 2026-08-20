package com.example.zhttaskflow.feature.article.presentation

import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.mvi.BaseViewModel
import com.example.zhttaskflow.base.mvi.getDataOrNull
import com.example.zhttaskflow.feature.article.domain.ArticlePage
import com.example.zhttaskflow.feature.article.domain.ArticlePagingDefaults
import com.example.zhttaskflow.feature.article.domain.ArticleRepository
import com.example.zhttaskflow.nav.route.TaskFlowArticleNavRoutes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * 资讯列表 ViewModel：MVI 单向数据流，调度 [ArticleRepository] 分页与 UI 状态。
 *
 * **调用方式**：UI 通过 [onEvent] 投递 [ArticleUiEvent]；订阅 [uiState] 渲染，订阅 [uiEffect] 处理 Toast/导航。
 *
 * **线程约束**：数据加载在 [launchTask] 内执行（仓库层 IO）；本类不直接访问网络/数据库 SDK。
 *
 * @param repository 由 [ArticleViewModelFactory] 手动注入
 */
class ArticleViewModel(
    private val repository: ArticleRepository,
) : BaseViewModel<ArticleUiState, ArticleUiEvent, ArticleUiEffect>(BaseUiState.Loading) {

    private val logTag = "ArticleViewModel"
    private val pageSize = ArticlePagingDefaults.DEFAULT_PAGE_SIZE

    init {
        loadFirstPage()
    }

    override fun handleEvent(event: ArticleUiEvent) {
        when (event) {
            ArticleUiEvent.Refresh -> refresh()
            ArticleUiEvent.LoadMore -> loadMore()
            is ArticleUiEvent.ArticleClicked -> navigateToDetail(event.articleId, event.detailUrl)
        }
    }

    private fun loadFirstPage() {
        setState { BaseUiState.Loading }
        launchTask(
            tag = logTag,
            onError = { throwable ->
                setState {
                    BaseUiState.Error(
                        throwable.message ?: "加载失败",
                    )
                }
                sendEffect(
                    ArticleUiEffect.ShowToast(
                        throwable.message ?: "加载失败",
                    ),
                )
            },
        ) {
            val page = repository.refreshArticlePage(
                page = ArticlePagingDefaults.FIRST_PAGE,
                pageSize = pageSize,
            )
            applyPageResult(page = page, append = false, isRefresh = false)
        }
    }

    private fun refresh() {
        val current = currentState
        if (current is BaseUiState.Success) {
            setState {
                BaseUiState.Success(
                    current.data.copy(
                        isRefreshing = true,
                        isLoadingMore = false,
                        isLoadMoreError = false,
                    ),
                )
            }
        }
        launchTask(
            tag = logTag,
            onError = { throwable ->
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
                            BaseUiState.Error(
                                throwable.message ?: "刷新失败",
                            )
                        }
                    }
                }
                sendEffect(
                    ArticleUiEffect.ShowToast(
                        throwable.message ?: "刷新失败",
                    ),
                )
            },
        ) {
            repository.clearMemoryCache()
            val page = repository.refreshArticlePage(
                page = ArticlePagingDefaults.FIRST_PAGE,
                pageSize = pageSize,
            )
            applyPageResult(page = page, append = false, isRefresh = true)
        }
    }

    private fun loadMore() {
        val state = currentState as? BaseUiState.Success ?: return
        val data = state.data
        if (!data.hasMore || data.isLoadingMore) {
            return
        }
        setState {
            BaseUiState.Success(
                data.copy(
                    isLoadingMore = true,
                    isLoadMoreError = false,
                ),
            )
        }
        val nextPage = data.currentPage + 1
        launchTask(
            tag = logTag,
            onError = { throwable ->
                setState {
                    BaseUiState.Success(
                        data.copy(
                            isLoadingMore = false,
                            isLoadMoreError = true,
                        ),
                    )
                }
                sendEffect(
                    ArticleUiEffect.ShowToast(
                        throwable.message ?: "加载更多失败",
                    ),
                )
            },
        ) {
            val page = repository.refreshArticlePage(page = nextPage, pageSize = pageSize)
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
            sendEffect(ArticleUiEffect.ShowToast("刷新成功"))
        }
    }

    private fun navigateToDetail(articleId: String, detailUrl: String) {
        if (articleId.isBlank() || detailUrl.isBlank()) {
            sendEffect(ArticleUiEffect.ShowToast("无法打开详情"))
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
}

/**
 * [ArticleViewModel] 手动注入工厂（无 Hilt）。
 */
class ArticleViewModelFactory(
    private val repository: ArticleRepository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticleViewModel::class.java)) {
            return ArticleViewModel(repository) as T
        }
        throw IllegalArgumentException("未知 ViewModel: ${modelClass.name}")
    }
}
