package com.example.zhttaskflow.feature.article.domain.usecase

import com.example.zhttaskflow.feature.article.domain.ArticlePage
import com.example.zhttaskflow.feature.article.domain.ArticlePagingDefaults
import com.example.zhttaskflow.feature.article.domain.ArticleRepository

/**
 * 下拉刷新资讯列表用例：先清空内存缓存，再强制从远程刷新并回写本地。
 *
 * **调用边界**：表现层下拉刷新时调用；与 [GetArticlePageUseCase] 区分在于必须清空内存层。
 */
class RefreshArticlePageUseCase(
    private val repository: ArticleRepository,
) {

    /**
     * @param page 页码（从 1 开始）
     * @param pageSize 每页条数
     */
    suspend operator fun invoke(
        page: Int,
        pageSize: Int = ArticlePagingDefaults.DEFAULT_PAGE_SIZE,
    ): ArticlePage {
        repository.clearMemoryCache()
        return repository.refreshArticlePage(page = page, pageSize = pageSize)
    }
}
