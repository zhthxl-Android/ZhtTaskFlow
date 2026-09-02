package com.example.zhttaskflow.feature.article.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.mvi.BaseViewModel
import com.example.zhttaskflow.core.foundation.TaskFlowNetworkException
import com.example.zhttaskflow.feature.article.domain.usecase.GetArticleDetailUseCase

/**
 * 资讯详情 ViewModel：加载校验与 StateBox 四态编排，WebView 渲染由 Composable 承担。
 */
class ArticleDetailViewModel(
    private val articleId: String,
    private val detailUrl: String,
    private val getArticleDetailUseCase: GetArticleDetailUseCase,
    private val networkUnavailableMessage: String,
) : BaseViewModel<ArticleDetailUiState, ArticleDetailUiEvent, ArticleDetailUiEffect>(
    BaseUiState.Loading,
) {

    private val logTag = "ArticleDetailViewModel"

    override fun handleEvent(event: ArticleDetailUiEvent) {
        when (event) {
            is ArticleDetailUiEvent.Load -> loadDetail(event.articleId, event.detailUrl)
            ArticleDetailUiEvent.Retry -> loadDetail(articleId, detailUrl)
        }
    }

    private fun loadDetail(id: String, url: String) {
        launchTask(
            tag = logTag,
            scene = "loadDetail",
            userMessageFallback = networkUnavailableMessage,
            onError = { throwable, message ->
                val displayMessage = if (throwable is TaskFlowNetworkException) {
                    networkUnavailableMessage
                } else {
                    message
                }
                setState { BaseUiState.Error(displayMessage) }
            },
        ) {
            setState { BaseUiState.Loading }
            val result = getArticleDetailUseCase(articleId = id, detailUrl = url)
            if (result == null) {
                setState { BaseUiState.Empty }
            } else {
                setState {
                    BaseUiState.Success(
                        ArticleDetailData(
                            articleId = result.articleId,
                            detailUrl = result.detailUrl,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * 资讯详情 ViewModel 工厂：由路由宿主注入用例与本地化无网文案。
 */
class ArticleDetailViewModelFactory(
    private val articleId: String,
    private val detailUrl: String,
    private val getArticleDetailUseCase: GetArticleDetailUseCase,
    private val networkUnavailableMessage: String,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArticleDetailViewModel::class.java)) {
            return ArticleDetailViewModel(
                articleId = articleId,
                detailUrl = detailUrl,
                getArticleDetailUseCase = getArticleDetailUseCase,
                networkUnavailableMessage = networkUnavailableMessage,
            ) as T
        }
        throw IllegalArgumentException("未知 ViewModel: ${modelClass.name}")
    }
}
