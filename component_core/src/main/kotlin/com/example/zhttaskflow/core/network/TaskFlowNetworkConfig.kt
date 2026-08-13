package com.example.zhttaskflow.core.network

/**
 * 全局网络客户端配置。
 *
 * 安全约定：[enableLogging] 仅在 Debug 构建为 true；Release 必须为 false，避免 BODY 日志泄露。
 * 默认使用有限超时，避免无限等待导致 ANR。
 */
data class TaskFlowNetworkConfig(
    val baseUrl: String,
    val connectTimeoutSeconds: Long = 30L,
    val readTimeoutSeconds: Long = 30L,
    val writeTimeoutSeconds: Long = 30L,
    val defaultHeaders: Map<String, String> = emptyMap(),
    val enableLogging: Boolean = false,
)
