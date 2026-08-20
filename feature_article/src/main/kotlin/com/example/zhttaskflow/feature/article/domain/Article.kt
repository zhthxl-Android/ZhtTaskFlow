package com.example.zhttaskflow.feature.article.domain

/**
 * 文章领域实体：纯 Kotlin 数据结构，不依赖 Android 与任何第三方库。
 *
 * @param id 文章唯一标识
 * @param title 标题
 * @param summary 摘要
 * @param coverUrl 封面图 URL（可为空）
 * @param author 作者
 * @param publishedAt 发布时间（毫秒时间戳）
 * @param category 分类/栏目
 * @param detailUrl 详情页 H5 链接（WebView 加载）
 */
data class Article(
    val id: String,
    val title: String,
    val summary: String,
    val coverUrl: String?,
    val author: String,
    val publishedAt: Long,
    val category: String,
    val detailUrl: String,
)
