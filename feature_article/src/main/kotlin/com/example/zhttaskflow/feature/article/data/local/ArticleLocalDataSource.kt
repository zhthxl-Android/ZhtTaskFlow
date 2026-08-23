package com.example.zhttaskflow.feature.article.data.local

import android.content.Context
import com.example.zhttaskflow.base.foundation.TaskFlowLogger
import com.example.zhttaskflow.core.network.isTaskFlowDebugLoggingEnabled
import com.example.zhttaskflow.core.persistence.room.TaskFlowRoomConfig
import com.example.zhttaskflow.core.persistence.room.TaskFlowRoomTemplate
import com.example.zhttaskflow.feature.article.data.ArticleDataConstants
import com.example.zhttaskflow.feature.article.data.mapper.ArticleMapper
import com.example.zhttaskflow.feature.article.domain.ArticlePage

private const val LOCAL_DATA_SOURCE_LOG_TAG = "ArticleLocalDataSource"

/**
 * 文章本地数据源：仅持有 [ArticleDao]，数据库操作统一经 [TaskFlowRoomTemplate]。
 *
 * **日志**：仅 Debug 级操作埋点；异常由 Room 模板包装后向上抛出，不在本层打印 Error。
 */
class ArticleLocalDataSource private constructor(
    private val articleDao: ArticleDao,
) {

    companion object {
        /**
         * 通过 core Room 统一门面创建本地数据源。
         */
        fun create(context: Context): ArticleLocalDataSource {
            val dao = TaskFlowRoomTemplate.openDao(
                context = context,
                config = TaskFlowRoomConfig(databaseName = ArticleDataConstants.DATABASE_NAME),
                databaseClass = ArticleDatabase::class.java,
                daoProvider = { database -> database.articleDao() },
            )
            return ArticleLocalDataSource(dao)
        }
    }

    /**
     * 读取某一页缓存；无缓存时返回 null。
     */
    suspend fun loadPage(page: Int, pageSize: Int): ArticlePage? {
        val cached = TaskFlowRoomTemplate.runWithDao(
            dao = articleDao,
            tag = LOCAL_DATA_SOURCE_LOG_TAG,
            block = { dao ->
                val entities = dao.getArticlesForPage(page)
                val meta = dao.getPageMeta(page)
                ArticleMapper.entitiesToDomainPage(entities, meta, page, pageSize)
            },
        )
        logLocalDebug(
            if (cached != null) {
                "读取缓存命中 page=$page size=$pageSize articles=${cached.articles.size}"
            } else {
                "读取缓存无数据 page=$page size=$pageSize"
            },
        )
        return cached
    }

    /**
     * 写入某一页缓存（整页替换）。
     */
    suspend fun savePage(page: ArticlePage) {
        TaskFlowRoomTemplate.runWithDao(
            dao = articleDao,
            tag = LOCAL_DATA_SOURCE_LOG_TAG,
            block = { dao ->
                val (entities, meta) = ArticleMapper.pageDomainToEntities(page)
                dao.replacePageCache(page.page, entities, meta)
            },
        )
        logLocalDebug(
            "写入缓存完成 page=${page.page} articles=${page.articles.size}",
        )
    }

    private fun logLocalDebug(message: String) {
        if (!isTaskFlowDebugLoggingEnabled()) {
            return
        }
        TaskFlowLogger.d(LOCAL_DATA_SOURCE_LOG_TAG, message)
    }
}
