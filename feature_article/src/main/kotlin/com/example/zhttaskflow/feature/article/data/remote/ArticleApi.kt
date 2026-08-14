package com.example.zhttaskflow.feature.article.data.remote

import com.example.zhttaskflow.feature.article.data.ArticleDataConstants
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 文章分页网络接口：仅声明 HTTP 契约，由 [com.example.zhttaskflow.core.network.RetrofitServiceFactory] 创建实例。
 */
interface ArticleApi {

    /**
     * 拉取分页文章列表。
     *
     * @param page 页码（从 1 开始）
     * @param pageSize 每页条数
     */
    @GET(ArticleDataConstants.API_PATH_ARTICLES)
    suspend fun getArticles(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): ArticlePageDto
}
