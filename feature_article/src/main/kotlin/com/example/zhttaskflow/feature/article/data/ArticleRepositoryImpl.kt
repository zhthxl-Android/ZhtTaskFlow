package com.example.zhttaskflow.feature.article.data

import com.example.zhttaskflow.core.cache.ThreeTierCache
import com.example.zhttaskflow.core.network.ApiResult
import com.example.zhttaskflow.feature.article.data.remote.ArticlePageRemoteFetcher
import com.example.zhttaskflow.feature.article.data.local.ArticleLocalDataSource
import com.example.zhttaskflow.feature.article.data.remote.ArticleRemoteDataSource
import com.example.zhttaskflow.feature.article.domain.ArticlePage
import com.example.zhttaskflow.feature.article.domain.ArticleRepository
import kotlinx.coroutines.flow.Flow

/**
 * [ArticleRepository] 实现：组合本地数据源、远程数据源与 [ThreeTierCache]。
 *
 * ## 防腐边界
 * - 业务仅定义领域模型与 Room **声明式**表结构/查询（见 `local` 包）；
 * - 数据库实例创建、DAO 获取、IO 线程与事务运行时由 **component_core** 统一封装；
 * - 本类不直接调用 [androidx.room.Room] 运行时 API。
 *
 * 网络侧经 [com.example.zhttaskflow.core.network.safeApiCall] 兜底。
 *
 * ## 三级缓存与刷新分工
 * - [observeArticlePage]：经 [ThreeTierCache.observe] 推送本地→内存→远程的渐进数据，适合订阅式 UI。
 * - [refreshArticlePage]：经 [ThreeTierCache.refresh] 强制走远程并回写本地与内存，适合首屏、下拉刷新与加载更多。
 * - [clearMemoryCache]：仅清空内存层，下拉刷新前调用以避免陈旧页数据干扰。
 */
class ArticleRepositoryImpl(
    private val localDataSource: ArticleLocalDataSource,
    private val remoteDataSource: ArticlePageRemoteFetcher,
) : ArticleRepository {

    private val pageTierCache = ThreeTierCache<ArticlePageCacheKey, ArticlePage>(
        readLocal = { key -> localDataSource.loadPage(key.page, key.pageSize) },
        readRemote = { key ->
            unwrapOrThrow(remoteDataSource.fetchPage(key.page, key.pageSize))
        },
        writeLocal = { _, page -> localDataSource.savePage(page) },
    )

    override fun observeArticlePage(page: Int, pageSize: Int): Flow<ArticlePage> {
        return pageTierCache.observe(ArticlePageCacheKey(page, pageSize))
    }

    override suspend fun refreshArticlePage(page: Int, pageSize: Int): ArticlePage {
        return pageTierCache.refresh(ArticlePageCacheKey(page, pageSize))
    }

    override suspend fun clearMemoryCache() {
        pageTierCache.clearMemory()
    }

    private fun <T> unwrapOrThrow(result: ApiResult<T>): T {
        return when (result) {
            is ApiResult.Success -> result.data
            is ApiResult.Failure -> throw result.exception
        }
    }
}
