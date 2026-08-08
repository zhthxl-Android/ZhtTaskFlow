package com.example.zhttaskflow.core.network

import com.example.zhttaskflow.base.foundation.TaskFlowNetworkException
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 将非 2xx 响应转为可识别的网络异常信息（业务 Feature 可再映射）。
 * 全局响应拦截器，统一处理返回码、业务错误码、数据前置解析等逻辑。
 */
class TaskFlowResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.isSuccessful) {
            return response
        }
        throw TaskFlowNetworkException(
            message = "HTTP ${response.code}: ${response.message}",
            code = response.code,
        )
    }
}
