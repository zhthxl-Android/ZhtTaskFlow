package com.example.zhttaskflow.core.network

/**
 * 全局网络客户端配置（遗留数据结构，供扩展场景引用默认超时等语义）。
 *
 * 标准接入请使用 [RetrofitServiceFactory.createApi]；日志开关由宿主 [Context.isAppDebuggable] 运行时判断。
 */
data class TaskFlowNetworkConfig(
    val baseUrl: String,
    val connectTimeoutSeconds: Long = 30L,
    val readTimeoutSeconds: Long = 30L,
    val writeTimeoutSeconds: Long = 30L,
    val defaultHeaders: Map<String, String> = emptyMap(),
    val enableLogging: Boolean = false,
)
