package com.example.zhttaskflow.feature.article.presentation

import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.feature.article.domain.Article

/**
 * 资讯列表页业务载荷（仅出现在 [BaseUiState.Success] 中）。
 */
data class ArticleListData(
    val articles: List<Article>,
    val currentPage: Int,
    val hasMore: Boolean,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadMoreError: Boolean = false,
)

/** 资讯列表页 UI 状态：`BaseUiState` 通用分支 + [ArticleListData] 业务数据。 */
typealias ArticleUiState = BaseUiState<ArticleListData>
