package com.example.zhttaskflow.feature.article.data.remote

import com.example.zhttaskflow.core.network.ApiResult
import com.example.zhttaskflow.core.network.safeApiCall
import com.example.zhttaskflow.feature.article.data.mapper.ArticleMapper
import com.example.zhttaskflow.feature.article.domain.ArticlePage

/**
 * 文章远程数据源：通过 [ArticleApi] 拉取分页数据，统一 [safeApiCall] 兜底。
 */
class ArticleRemoteDataSource(
    private val articleApi: ArticleApi,
) {

    private val logTag = "ArticleRemoteDataSource"

    /**
     * 拉取远程分页数据并映射为领域分页模型。
     *
     * @return [ApiResult] 成功时为 [ArticlePage]，失败时含分类错误信息
     */
    suspend fun fetchPage(page: Int, pageSize: Int): ApiResult<ArticlePage> {
        return safeApiCall(tag = logTag) {
            val dto = articleApi.getArticles(page = page, pageSize = pageSize)
            ArticleMapper.pageDtoToDomain(dto)
        }
    }
}
