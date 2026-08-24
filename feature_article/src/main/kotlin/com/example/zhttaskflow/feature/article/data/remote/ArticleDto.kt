package com.example.zhttaskflow.feature.article.data.remote

import com.google.gson.annotations.SerializedName

/**
 * 玩 Android 列表接口外层包装：`data` + `errorCode` + `errorMsg`。
 */
data class WanAndroidArticleListResponse(
    @SerializedName("data")
    val articlePage: ArticlePageDto?,
    @SerializedName("errorCode")
    val errorCode: Int?,
    @SerializedName("errorMsg")
    val errorMsg: String?,
)

/**
 * 分页数据块（对应 JSON 内 `data` 对象）。
 */
data class ArticlePageDto(
    @SerializedName("curPage")
    val curPage: Int?,
    @SerializedName("datas")
    val articleList: List<ArticleItemDto>,
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
 * 网络层文章条目 DTO（玩 Android `datas` 元素）。
 */
data class ArticleItemDto(
    @SerializedName("author")
    val author: String?,
    @SerializedName("id")
    val id: Int?,
    @SerializedName("link")
    val link: String?,
    @SerializedName("niceDate")
    val niceDate: String?,
    @SerializedName("niceShareDate")
    val niceShareDate: String?,
    @SerializedName("title")
    val title: String?,
    @SerializedName("userId")
    val userId: Int?,
    @SerializedName("zan")
    val zan: Int?,
)
