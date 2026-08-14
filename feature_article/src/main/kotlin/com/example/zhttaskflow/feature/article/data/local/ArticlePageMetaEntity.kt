package com.example.zhttaskflow.feature.article.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.zhttaskflow.feature.article.data.ArticleDataConstants

/**
 * 分页元信息本地实体。
 *
 * **防腐说明**：仅 [@Entity] 声明；运行时由 core 层 Room 工厂与 IO 封装访问。
 */
@Entity(tableName = ArticleDataConstants.TABLE_ARTICLE_PAGE_META)
data class ArticlePageMetaEntity(
    @PrimaryKey
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean,
)
