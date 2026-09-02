package com.example.zhttaskflow.feature.article.domain.usecase

import com.example.zhttaskflow.core.util.isNotNullOrBlank

/**
 * 资讯详情加载用例：校验路由参数并产出 WebView 所需领域载荷。
 *
 * 网络可达性由表现层 [com.example.zhttaskflow.base.mvi.BaseViewModel.launchTask] 预检；
 * 本用例不直接访问 Android 网络 API 或 Retrofit。
 */
class GetArticleDetailUseCase {

    /**
     * @return 参数合法时返回 [ArticleDetailResult]；否则 `null`（对应 UI Empty 态）
     */
    suspend operator fun invoke(
        articleId: String,
        detailUrl: String,
    ): ArticleDetailResult? {
        if (!articleId.isNotNullOrBlank() || !detailUrl.isNotNullOrBlank()) {
            return null
        }
        return ArticleDetailResult(
            articleId = articleId.trim(),
            detailUrl = detailUrl.trim(),
        )
    }
}

/**
 * 资讯详情领域结果（供表现层映射为 [com.example.zhttaskflow.feature.article.presentation.ArticleDetailData]）。
 */
data class ArticleDetailResult(
    val articleId: String,
    val detailUrl: String,
)
