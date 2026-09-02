package com.example.zhttaskflow.feature.article.data.remote

import com.google.gson.annotations.SerializedName

/**
 * 玩 Android 分页 `data` 块（嵌套于 [com.example.zhttaskflow.core.network.ApiResponse.data]）。
 *
 * 字段均按服务端契约可空声明；集合与展示数据在 [com.example.zhttaskflow.feature.article.data.mapper.ArticleMapper] 兜底。
 */
data class ArticlePageDto(
    @SerializedName("curPage")
    val curPage: Int?,
    @SerializedName("datas")
    val articleList: List<ArticleItemDto>?,
    @SerializedName("offset")
    val offset: Int?,
    @SerializedName("over")
    val over: Boolean?,
    @SerializedName("pageCount")
    val pageCount: Int?,
    @SerializedName("size")
    val size: Int?,
    @SerializedName("total")
    val total: Int?,
)

/**
 * 玩 Android 文章列表单条 `datas` 元素。
 */
data class ArticleItemDto(
    @SerializedName("apkLink")
    val apkLink: String?,
    @SerializedName("author")
    val author: String?,
    @SerializedName("chapterId")
    val chapterId: Int?,
    @SerializedName("chapterName")
    val chapterName: String?,
    @SerializedName("collect")
    val collect: Boolean?,
    @SerializedName("courseId")
    val courseId: Int?,
    @SerializedName("desc")
    val desc: String?,
    @SerializedName("envelopePic")
    val envelopePic: String?,
    @SerializedName("fresh")
    val fresh: Boolean?,
    @SerializedName("id")
    val id: Int?,
    @SerializedName("link")
    val link: String?,
    @SerializedName("niceDate")
    val niceDate: String?,
    @SerializedName("niceShareDate")
    val niceShareDate: String?,
    @SerializedName("origin")
    val origin: String?,
    @SerializedName("prefix")
    val prefix: String?,
    @SerializedName("projectType")
    val projectType: Int?,
    @SerializedName("publishTime")
    val publishTime: Long?,
    @SerializedName("selfVisible")
    val selfVisible: Int?,
    @SerializedName("shareDate")
    val shareDate: Long?,
    @SerializedName("shareUser")
    val shareUser: String?,
    @SerializedName("superChapterId")
    val superChapterId: Int?,
    @SerializedName("superChapterName")
    val superChapterName: String?,
    @SerializedName("tags")
    val tags: List<ArticleTagDto>?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("type")
    val type: Int?,
    @SerializedName("userId")
    val userId: Int?,
    @SerializedName("visible")
    val visible: Int?,
    @SerializedName("zan")
    val zan: Int?,
)

/**
 * 文章标签（玩 Android `tags` 元素，按需解析）。
 */
data class ArticleTagDto(
    @SerializedName("name")
    val name: String?,
    @SerializedName("url")
    val url: String?,
)
