package com.example.zhttaskflow.feature.article.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.zhttaskflow.feature.article.data.ArticleDataConstants

/**
 * 文章本地 Room 实体。
 *
 * **防腐说明**：仅使用 Room 编译期 [@Entity] 元数据；运行时读写经 core 封装与 [ArticleDao] 完成。
 */
@Entity(tableName = ArticleDataConstants.TABLE_ARTICLES)
data class ArticleEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val summary: String,
    val coverUrl: String?,
    val author: String,
    val publishedAt: Long,
    /** 所属列表页码（缓存分区） */
    val listPage: Int,
    /** 页内排序位置 */
    val listPosition: Int,
)
