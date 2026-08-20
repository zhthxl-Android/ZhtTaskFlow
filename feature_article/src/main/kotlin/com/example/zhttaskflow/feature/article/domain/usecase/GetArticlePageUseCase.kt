package com.example.zhttaskflow.feature.article.domain.usecase

import com.example.zhttaskflow.feature.article.domain.ArticlePage
import com.example.zhttaskflow.feature.article.domain.ArticlePagingDefaults
import com.example.zhttaskflow.feature.article.domain.ArticleRepository

/**
 * 分页拉取资讯列表用例：经 [ArticleRepository.refreshArticlePage] 走三级缓存（内存 → 本地 → 网络）。
 *
 * **调用边界**：表现层首屏加载、加载更多时调用；不包含 UI 状态编排。
 */
class GetArticlePageUseCase(
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
        return repository.refreshArticlePage(page = page, pageSize = pageSize)
    }
}
