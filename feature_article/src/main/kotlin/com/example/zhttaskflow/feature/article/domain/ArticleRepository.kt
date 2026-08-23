package com.example.zhttaskflow.feature.article.domain

import kotlinx.coroutines.flow.Flow

/**
 * 文章仓库抽象：定义列表观察、刷新与分页能力，由 data 层实现。
 *
 * 遵循依赖倒置：仅表达业务语义，不暴露网络、数据库等实现细节。
 */
interface ArticleRepository {

    /**
     * 观察指定页的文章分页数据流。
     *
     * 数据按「内存 → 本地 → 网络」顺序发射；网络失败时保留已发射的本地/内存数据（由 core 三级缓存保证）。
     *
     * @param page 页码（从 1 开始）
     * @param pageSize 每页条数
     */
    fun observeArticlePage(
        page: Int,
        pageSize: Int = ArticlePagingDefaults.DEFAULT_PAGE_SIZE,
    ): Flow<ArticlePage>

    /**
     * 强制从网络刷新指定页并回写本地与内存缓存。
     *
     * @param page 页码（从 1 开始）
     * @param pageSize 每页条数
     * @return 刷新后的分页结果
     * @throws com.example.zhttaskflow.core.foundation.TaskFlowException 网络或持久化失败时
     */
    suspend fun refreshArticlePage(
        page: Int,
        pageSize: Int = ArticlePagingDefaults.DEFAULT_PAGE_SIZE,
    ): ArticlePage

    /**
     * 清空列表三级缓存中的内存层，下拉刷新前调用。
     */
    suspend fun clearMemoryCache()
}
