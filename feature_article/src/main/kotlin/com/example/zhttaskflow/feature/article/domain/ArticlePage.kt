package com.example.zhttaskflow.feature.article.domain

/**
 * 分页文章列表的领域聚合：承载一页数据及分页元信息。
 *
 * @param articles 当前页文章列表
 * @param page 页码（从 1 开始）
 * @param pageSize 每页条数
 * @param hasMore 是否仍有下一页
 */
data class ArticlePage(
    val articles: List<Article>,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean,
)
