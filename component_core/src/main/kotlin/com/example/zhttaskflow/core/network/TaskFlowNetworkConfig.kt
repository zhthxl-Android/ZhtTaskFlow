package com.example.zhttaskflow.core.network

/**
 * 全局网络客户端配置（历史兼容数据结构）。
 *
 * @deprecated 请使用 [RetrofitServiceFactory.createApi] 创建 API Service；
 * 超时、日志等策略已由工厂与 [TaskFlowNetworkDefaults] 统一维护。
 */
@Deprecated(
    message = "请使用 RetrofitServiceFactory.createApi 创建网络 API",
    replaceWith = ReplaceWith(
        expression = "RetrofitServiceFactory.createApi(context, baseUrl, serviceClass)",
        imports = ["com.example.zhttaskflow.core.network.RetrofitServiceFactory"],
    ),
)
data class TaskFlowNetworkConfig(
    val baseUrl: String,
    val connectTimeoutSeconds: Long = 30L,
    val readTimeoutSeconds: Long = 30L,
    val writeTimeoutSeconds: Long = 30L,
    val defaultHeaders: Map<String, String> = emptyMap(),
    val enableLogging: Boolean = false,
)
