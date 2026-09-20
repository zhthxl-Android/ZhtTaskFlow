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
import com.example.zhttaskflow.nav.route.ArticleNavRoutes

/**
 * 资讯列表 ViewModel：MVI 单向数据流，通过领域用例调度分页与 UI 状态。
 *
 * **调用方式**：UI 通过 [onEvent] 投递 [ArticleUiEvent]；订阅 [uiState] 渲染，订阅 [uiEffect] 处理 Snackbar/导航。
 *
 * **线程约束**：数据加载在 [launchTask] 内执行（用例 → 仓库 IO）；本类不直接访问 Repository 与网络/数据库 SDK。
 */
class ArticleViewModel(
    private val getArticlePageUseCase: GetArticlePageUseCase,//普通分页加载用例
    private val refreshArticlePageUseCase: RefreshArticlePageUseCase,//下拉刷新用例
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
            is ArticleUiEvent.ArticleClicked -> navigateToDetail(
                event.articleId,
                event.detailUrl
            )
        }
    }

    // endregion

    // region 资讯列表加载

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
                    ArticleUiEffect.ShowSnackbar(
                        message = message,
                        type = SnackbarType.Error
                    ),
                )
            },
        ) {
            //请求数据
            val page = getArticlePageUseCase(
                page = ArticlePagingDefaults.FIRST_PAGE,
                pageSize = pageSize,
            )
            //处理返回数据
            applyPageResult(
                page = page,
                append = false,
                isRefresh = false
            )
        }
    }

    // endregion

    //验证新翻墙工具本地push

    // region 下拉刷新

    private fun refresh() {
        val current = currentState
        if (current is BaseUiState.Success) {
            //设置当前状态为下拉刷新
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
                        //成功状态下刷新失败
                        setState {
                            //只取消刷新状态，保留原有数据
                            BaseUiState.Success(
                                state.data.copy(isRefreshing = false),
                            )
                        }
                    }

                    else -> {
                        //非成功态下刷新失败：才显示错误页
                        setState {
                            BaseUiState.Error(message)
                        }
                    }
                }
                sendEffect(
                    ArticleUiEffect.ShowSnackbar(
                        message = message,
                        type = SnackbarType.Error
                    ),
                )
            },
        ) {
            val page = refreshArticlePageUseCase(
                page = ArticlePagingDefaults.FIRST_PAGE,
                pageSize = pageSize,
            )
            applyPageResult(
                page = page,
                append = false,
                isRefresh = true
            )
        }
    }

    // endregion

    // region 加载更多

    private fun loadMore() {
        //不是成功态直接返回：没数据没法加载更多
        val state = currentState as? BaseUiState.Success ?: return
        val data = state.data
        //没有更多数据、或者正在加载中，直接返回：防止重复请求
        if (!data.hasMore || data.isLoadingMore) {
            return
        }
        //设置当前状态为加载更多
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
                    //设置加载更多失败
                    BaseUiState.Success(data.withLoadMoreError())
                }
                sendEffect(
                    ArticleUiEffect.ShowSnackbar(
                        message = message,
                        type = SnackbarType.Error
                    ),
                )
            },
        ) {
            val page = getArticlePageUseCase(
                page = nextPage,
                pageSize = pageSize
            )
            applyPageResult(
                page = page,
                append = true,
                isRefresh = false
            )
        }
    }

    // endregion

    // region 结果统一处理

    private fun applyPageResult(
        page: ArticlePage,
        append: Boolean,//是否是追加，true = 追加到原有列表后面（加载更多用）；false = 直接替换整个列表（首屏、刷新用）
        isRefresh: Boolean,//是否是下拉刷新，成功后是否弹「刷新成功」提示
    ) {
        //如果数据为null，并且非追加，返回空数据状态
        if (page.articles.isEmpty() && !append) {
            //空态
            setState { BaseUiState.Empty }
            return
        }
        //获取成功状态下的数据，非成功态返回null
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
        //只有是下拉刷新、且数据不为空时，才弹「刷新成功」
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

    private fun navigateToDetail(
        articleId: String,
        detailUrl: String
    ) {
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
                url = ArticleNavRoutes.detailPath(
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
