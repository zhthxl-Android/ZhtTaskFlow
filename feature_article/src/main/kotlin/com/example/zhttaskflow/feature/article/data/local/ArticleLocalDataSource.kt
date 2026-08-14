package com.example.zhttaskflow.feature.article.data.local

import android.content.Context
import com.example.zhttaskflow.core.persistence.room.TaskFlowRoomConfig
import com.example.zhttaskflow.core.persistence.room.TaskFlowRoomTemplate
import com.example.zhttaskflow.feature.article.data.ArticleDataConstants
import com.example.zhttaskflow.feature.article.data.mapper.ArticleMapper
import com.example.zhttaskflow.feature.article.domain.ArticlePage

private const val LOCAL_DATA_SOURCE_LOG_TAG = "ArticleLocalDataSource"

/**
 * 文章本地数据源：仅持有 [ArticleDao]，数据库操作统一经 [TaskFlowRoomTemplate]（与网络层 [safeApiCall] 范式对齐）。
 *
 * DAO 由 [TaskFlowRoomTemplate.openDao] 获取；不持有 [androidx.room.RoomDatabase]。
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
    suspend fun loadPage(page: Int, pageSize: Int): ArticlePage? =
        TaskFlowRoomTemplate.runWithDao(
            dao = articleDao,
            tag = LOCAL_DATA_SOURCE_LOG_TAG,
            block = { dao ->
                val entities = dao.getArticlesForPage(page)
                val meta = dao.getPageMeta(page)
                ArticleMapper.entitiesToDomainPage(entities, meta, page, pageSize)
            },
        )

    /**
     * 写入某一页缓存（整页替换）。
     */
    suspend fun savePage(page: ArticlePage) =
        TaskFlowRoomTemplate.runWithDao(
            dao = articleDao,
            tag = LOCAL_DATA_SOURCE_LOG_TAG,
            block = { dao ->
                val (entities, meta) = ArticleMapper.pageDomainToEntities(page)
                dao.replacePageCache(page.page, entities, meta)
            },
        )
}
