package com.example.zhttaskflow.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * 统一响应透传拦截器，预留鉴权刷新、全局错误码处理扩展点。
 */
internal class TaskFlowResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(chain.request())
    }
}
