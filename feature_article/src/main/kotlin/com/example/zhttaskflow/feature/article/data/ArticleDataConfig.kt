package com.example.zhttaskflow.feature.article.data

/**
 * 文章数据层装配配置：由 Application 或 standalone 入口传入。
 *
 * @param baseUrl Retrofit 根地址，必须以 `/` 结尾；API 实例由 [com.example.zhttaskflow.core.network.RetrofitServiceFactory.createApi] 创建
 */
data class ArticleDataConfig(
    val baseUrl: String,
)
