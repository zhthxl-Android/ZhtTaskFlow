package com.example.zhttaskflow.feature.article.data

/**
 * 文章数据层常量：网络路径、数据库名等（无硬编码魔法字符串散落）。
 */
internal object ArticleDataConstants {
    const val DATABASE_NAME = "article_feature.db"
    const val API_PATH_ARTICLES = "articles"

    /** 列表默认分类展示文案（DTO 未返回时使用） */
    const val DEFAULT_CATEGORY = "资讯"

    /** 详情链接缺省时的占位域名（仅演示/兜底） */
    const val DEFAULT_DETAIL_URL_PREFIX = "https://example.com/article/"

    /** 文章列表缓存表（单条文章记录，含分页缓存列） */
    const val TABLE_ARTICLES = "articles"

    /** 分页元信息表（记录每页 hasMore 等） */
    const val TABLE_ARTICLE_PAGE_META = "article_page_meta"
}
