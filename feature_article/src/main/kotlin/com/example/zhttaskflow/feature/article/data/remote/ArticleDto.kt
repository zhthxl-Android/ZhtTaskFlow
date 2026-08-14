package com.example.zhttaskflow.feature.article.data.remote

/**
 * 网络层文章条目 DTO（与 Gson 字段映射）。
 */
data class ArticleDto(
    val id: String,
    val title: String,
    val summary: String,
    val coverUrl: String?,
    val author: String,
    val publishedAt: Long,
)

/**
 * 标准分页响应 DTO。
 */
data class ArticlePageDto(
    val list: List<ArticleDto>,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean,
)
