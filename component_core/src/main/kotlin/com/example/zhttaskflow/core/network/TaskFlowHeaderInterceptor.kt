package com.example.zhttaskflow.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 为每个请求注入调用方提供的默认请求头（由 [RetrofitServiceFactory.createApi] 的 `defaultHeaders` 或工厂内置空映射）。
 */
internal class TaskFlowHeaderInterceptor(
    private val headerProvider: () -> Map<String, String>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        headerProvider().forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        return chain.proceed(requestBuilder.build())
    }
}
