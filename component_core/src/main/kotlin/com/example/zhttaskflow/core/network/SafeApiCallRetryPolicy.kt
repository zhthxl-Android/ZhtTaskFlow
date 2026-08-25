package com.example.zhttaskflow.core.network

import com.google.gson.JsonIOException
import com.google.gson.JsonParseException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

/**
 * 默认仅重试网络层瞬时异常，不重试业务失败与 JSON 解析错误。
 */
fun isDefaultRetryableNetworkThrowable(throwable: Throwable): Boolean {
    return when (throwable) {
        is SocketTimeoutException,
        is UnknownHostException,
        -> true
        is JsonParseException,
        is JsonIOException,
        is HttpException,
        -> false
        is IOException -> true
        else -> false
    }
}

/**
 * [safeApiCall] 网络波动自动重试策略（默认仅对典型网络 IO 异常重试 1 次）。
 *
 * @param maxRetries 额外重试次数（默认 1 表示最多执行 2 次请求）
 * @param delayMillis 重试前短延迟，降低瞬时波动误杀概率
 * @param retryOn 是否对给定异常重试；业务异常、解析错误默认不重试
 */
data class SafeApiCallRetryPolicy(
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    val delayMillis: Long = DEFAULT_DELAY_MILLIS,
    val retryOn: (Throwable) -> Boolean = { isDefaultRetryableNetworkThrowable(it) },
) {

    companion object {
        const val DEFAULT_MAX_RETRIES: Int = 1
        const val DEFAULT_DELAY_MILLIS: Long = 200L

        val Default: SafeApiCallRetryPolicy = SafeApiCallRetryPolicy()
    }
}
