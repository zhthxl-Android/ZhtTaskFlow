package com.example.zhttaskflow.core.network

/**
 * OkHttp / Retrofit 构建配置。
 */
data class TaskFlowNetworkConfig(
    val baseUrl: String,
    val connectTimeoutSeconds: Long = 30L,
    val readTimeoutSeconds: Long = 30L,
    val writeTimeoutSeconds: Long = 30L,
    val enableLogging: Boolean = true,
    val defaultHeaders: Map<String, String> = emptyMap(),
)
