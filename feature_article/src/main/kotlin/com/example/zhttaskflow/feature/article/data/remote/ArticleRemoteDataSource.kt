package com.example.zhttaskflow.feature.article.data.remote

import com.example.zhttaskflow.core.network.ApiResult
import com.example.zhttaskflow.core.network.safeApiCall
import com.example.zhttaskflow.feature.article.data.ArticleDataConstants
import com.example.zhttaskflow.feature.article.data.mapper.ArticleMapper
import com.example.zhttaskflow.feature.article.domain.Article
import com.example.zhttaskflow.feature.article.domain.ArticlePage

/**
 * 文章分页远程数据拉取抽象，便于网络实现与演示数据切换。
 */
interface ArticlePageRemoteFetcher {

    suspend fun fetchPage(page: Int, pageSize: Int): ApiResult<ArticlePage>
}

/**
 * 远程分页数据源：通过 [ArticleApi] 拉取数据。
 */
class ArticleRemoteDataSource(
    private val articleApi: ArticleApi,
) : ArticlePageRemoteFetcher {

    private val logTag = "ArticleRemoteDataSource"

    override suspend fun fetchPage(page: Int, pageSize: Int): ApiResult<ArticlePage> {
        return safeApiCall(tag = logTag) {
            val dto = articleApi.getArticles(page = page, pageSize = pageSize)
            ArticleMapper.pageDtoToDomain(dto)
        }
    }
}

/**
 * 演示分页数据源：standalone 调试无后端时使用（模块内可见）。
 */
internal class ArticleMockRemoteDataSource : ArticlePageRemoteFetcher {

    override suspend fun fetchPage(page: Int, pageSize: Int): ApiResult<ArticlePage> {
        return safeApiCall(tag = "ArticleMockRemoteDataSource") {
            buildDemoPage(page, pageSize)
        }
    }

    private fun buildDemoPage(page: Int, pageSize: Int): ArticlePage {
        val now = System.currentTimeMillis()
        val articles = (1..pageSize).map { index ->
            val id = "demo-$page-$index"
            Article(
                id = id,
                title = "示例资讯 $page-$index",
                summary = "这是用于独立调试的示例摘要内容。",
                coverUrl = null,
                author = "TaskFlow",
                publishedAt = now - index * 60_000L,
                category = ArticleDataConstants.DEFAULT_CATEGORY,
                detailUrl = "${ArticleDataConstants.DEFAULT_DETAIL_URL_PREFIX}$id",
            )
        }
        val hasMore = page < 3
        return ArticlePage(
            articles = articles,
            page = page,
            pageSize = pageSize,
            hasMore = hasMore,
        )
    }
}
