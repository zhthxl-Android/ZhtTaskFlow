package com.example.zhttaskflow.feature.article.presentation

import com.example.zhttaskflow.base.mvi.BaseUiState

/**
 * 资讯详情页业务载荷（仅出现在 [BaseUiState.Success] 中）。
 *
 * @param articleId 文章标识
 * @param detailUrl WebView 加载的 H5 链接
 */
data class ArticleDetailData(
    val articleId: String,
    val detailUrl: String,
)

/** 资讯详情页 UI 状态。 */
typealias ArticleDetailUiState = BaseUiState<ArticleDetailData>
