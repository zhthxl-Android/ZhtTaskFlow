package com.example.zhttaskflow.feature.article.data

/**
 * 三级缓存分页键：区分不同页码与页大小。
 */
internal data class ArticlePageCacheKey(
    val page: Int,
    val pageSize: Int,
)
