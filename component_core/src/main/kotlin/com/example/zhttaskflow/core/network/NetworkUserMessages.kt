package com.example.zhttaskflow.core.network

/**
 * 网络层统一用户可见文案（与 [com.example.zhttaskflow.core.foundation.NetworkException.userMessage] 对齐）。
 */
internal object NetworkUserMessages {
    const val NETWORK_TIMEOUT: String = "网络连接超时，请检查网络后重试"
    const val NETWORK_IO: String = "网络连接失败，请检查网络设置"
    const val PARSE: String = "数据解析失败，请稍后重试"
    const val BUSINESS_FALLBACK: String = "请求失败，请稍后重试"
    const val UNKNOWN: String = "加载失败，请稍后重试"
}
