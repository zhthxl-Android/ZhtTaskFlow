package com.example.zhttaskflow.feature.article.data

/**
 * 文章数据层常量：网络路径、数据库名等（无硬编码魔法字符串散落）。
 */
internal object ArticleDataConstants {
    const val DATABASE_NAME = "article_feature.db"
    const val API_PATH_ARTICLES = "articles"

    /** 文章列表缓存表（单条文章记录，含分页缓存列） */
    const val TABLE_ARTICLES = "articles"

    /** 分页元信息表（记录每页 hasMore 等） */
    const val TABLE_ARTICLE_PAGE_META = "article_page_meta"
}
