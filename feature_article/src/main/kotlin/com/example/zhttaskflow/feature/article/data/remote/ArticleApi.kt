package com.example.zhttaskflow.feature.article.data.remote

import com.example.zhttaskflow.core.network.ApiResponse
import com.example.zhttaskflow.feature.article.api.ArticleApiPaths
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 文章分页网络接口：仅声明 HTTP 契约，实例由 [com.example.zhttaskflow.core.network.RetrofitServiceFactory.createApi] 提供。
 */
interface ArticleApi {

    /**
     * 拉取玩 Android 文章分页列表。
     *
     * @param page 页码，从 **0** 开始，对应路径占位符 `{page}`
     * @param pageSize 每页条数，对应 query 参数 `page_size`（取值 1–40）
     */
    @GET(ArticleApiPaths.ARTICLE_LIST)
    suspend fun getArticles(
        @Path("page") page: Int,
        @Query("page_size") pageSize: Int,
    ): ApiResponse<ArticlePageDto>
}
