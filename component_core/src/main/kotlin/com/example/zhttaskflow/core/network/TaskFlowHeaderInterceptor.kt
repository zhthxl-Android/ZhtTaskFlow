package com.example.zhttaskflow.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 为每个请求附加通用 Header（如鉴权 Token 由调用方注入）。
 */
class TaskFlowHeaderInterceptor(
    private val headersProvider: () -> Map<String, String>,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        headersProvider().forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        return chain.proceed(requestBuilder.build())
    }
}
