package com.example.zhttaskflow.feature.article.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.zhttaskflow.core.persistence.room.BaseRoomDao
import com.example.zhttaskflow.feature.article.data.ArticleDataConstants

/**
 * 文章本地 DAO：全部使用参数化查询，无 SQL 拼接。
 *
 * **防腐说明**：本文件为业务数据 **声明**（编译期 Room 注解）；运行时由 core 层
 * [com.example.zhttaskflow.core.persistence.room.RoomTemplate] 执行。
 */
@Dao
interface ArticleDao : BaseRoomDao {

    @Query("SELECT * FROM ${ArticleDataConstants.TABLE_ARTICLES} WHERE listPage = :page ORDER BY listPosition ASC")
    suspend fun getArticlesForPage(page: Int): List<ArticleEntity>

    @Query("SELECT * FROM ${ArticleDataConstants.TABLE_ARTICLE_PAGE_META} WHERE page = :page LIMIT 1")
    suspend fun getPageMeta(page: Int): ArticlePageMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(entities: List<ArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageMeta(meta: ArticlePageMetaEntity)

    @Query("DELETE FROM ${ArticleDataConstants.TABLE_ARTICLES} WHERE listPage = :page")
    suspend fun deleteArticlesForPage(page: Int)

    @Query("DELETE FROM ${ArticleDataConstants.TABLE_ARTICLE_PAGE_META} WHERE page = :page")
    suspend fun deletePageMeta(page: Int)

    /**
     * 替换某一页缓存：先删后插，保证页内数据一致。
     */
    @Transaction
    suspend fun replacePageCache(
        page: Int,
        articles: List<ArticleEntity>,
        meta: ArticlePageMetaEntity,
    ) {
        deleteArticlesForPage(page)
        deletePageMeta(page)
        insertArticles(articles)
        insertPageMeta(meta)
    }
}
