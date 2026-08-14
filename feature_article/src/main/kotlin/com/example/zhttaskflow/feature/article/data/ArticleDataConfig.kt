package com.example.zhttaskflow.feature.article.data

/**
 * 文章数据层装配配置：由 Application 或 standalone 入口传入。
 *
 * @param baseUrl Retrofit 根地址，必须以 `/` 结尾
 * @param enableNetworkLogging 是否开启 BODY 级网络日志（仅 Debug 应为 true）
 */
data class ArticleDataConfig(
    val baseUrl: String,
    val enableNetworkLogging: Boolean = false,
)
