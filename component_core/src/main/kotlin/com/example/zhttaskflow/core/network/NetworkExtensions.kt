package com.example.zhttaskflow.core.network

import com.example.zhttaskflow.core.foundation.TaskFlowNetworkException
import com.example.zhttaskflow.core.util.nullIfBlank
import com.google.gson.JsonIOException
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * 在 IO 线程执行挂起块，并包装为 [ApiResult]；区分网络 / 业务 / 解析异常。
 *
 * **前置检查**：已绑定 Application 上下文时，无可用网络则毫秒级失败，不发起真实请求。
 * **自动重试**：默认对网络 IO 类异常额外重试 1 次（200ms 间隔），业务错误不重试。
 *
 * **日志**：Release 不打印 Error；Debug 安装包输出一条 Debug 级详细日志（含堆栈）。业务 Error 由 ViewModel [launchTask] 统一记录。
 *
 * 当 [block] 返回 [ApiResponse] 时，请使用 [safeApiCallResponse] 自动拆包，无需手动 [unwrapApiResponse]。
 *
 * @param tag 日志 Tag
 * @param retryPolicy 重试策略，默认 [SafeApiCallRetryPolicy.Default]
 * @param block 网络或 IO 挂起调用
 */
suspend fun <T> safeApiCall(
    tag: String = "safeApiCall",
    retryPolicy: SafeApiCallRetryPolicy = SafeApiCallRetryPolicy.Default,
    block: suspend () -> T,
): ApiResult<T> = executeSafeApiCall(tag, retryPolicy, block)

/**
 * [block] 返回标准外层 [ApiResponse] 时自动拆包：成功 [ApiResult] 直接承载业务 DTO。
 */
suspend fun <T> safeApiCallResponse(
    tag: String = "safeApiCall",
    retryPolicy: SafeApiCallRetryPolicy = SafeApiCallRetryPolicy.Default,
    block: suspend () -> ApiResponse<T>,
): ApiResult<T> = executeSafeApiCall(tag, retryPolicy) {
    unwrapApiResponse(block())
}

internal suspend fun <T> executeSafeApiCall(
    tag: String,
    retryPolicy: SafeApiCallRetryPolicy,
    block: suspend () -> T,
): ApiResult<T> = withContext(Dispatchers.IO) {
    try {
        ApiResult.Success(
            runSafeApiCallBlock(tag = tag, retryPolicy = retryPolicy, block = block),
        )
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (timeout: SocketTimeoutException) {
        logSafeApiCallFailure(tag, "连接超时: ${timeout.message}", timeout)
        ApiResult.Failure(
            exception = networkException(
                technical = "SocketTimeoutException: ${timeout.message.nullIfBlank() ?: "timeout"}",
                userMessage = TaskFlowNetworkUserMessages.NETWORK_TIMEOUT,
                cause = timeout,
            ),
            kind = ApiErrorKind.NETWORK,
        )
    } catch (parseError: JsonParseException) {
        logSafeApiCallFailure(tag, "JSON 解析失败", parseError)
        parseFailure(parseError)
    } catch (parseIo: JsonIOException) {
        logSafeApiCallFailure(tag, "JSON IO 异常", parseIo)
        parseFailure(parseIo)
    } catch (httpException: HttpException) {
        val code = httpException.code()
        val message = httpException.message().nullIfBlank() ?: "HTTP $code"
        logSafeApiCallFailure(tag, "HTTP 失败 code=$code message=$message", httpException)
        val kind = if (code in 400..499) {
            ApiErrorKind.BUSINESS
        } else {
            ApiErrorKind.NETWORK
        }
        val userMessage = if (kind == ApiErrorKind.BUSINESS) {
            TaskFlowNetworkUserMessages.BUSINESS_FALLBACK
        } else {
            TaskFlowNetworkUserMessages.NETWORK_IO
        }
        ApiResult.Failure(
            exception = TaskFlowNetworkException(
                message = "HttpException code=$code message=$message",
                cause = httpException,
                errorCode = code,
                userMessage = userMessage,
            ),
            code = code,
            kind = kind,
        )
    } catch (io: IOException) {
        logSafeApiCallFailure(tag, "网络 IO 异常: ${io.message}", io)
        ApiResult.Failure(
            exception = networkException(
                technical = "IOException: ${io.message.nullIfBlank() ?: "network io"}",
                userMessage = TaskFlowNetworkUserMessages.NETWORK_IO,
                cause = io,
            ),
            kind = ApiErrorKind.NETWORK,
        )
    } catch (network: TaskFlowNetworkException) {
        logSafeApiCallFailure(
            tag,
            network.message.nullIfBlank() ?: "业务请求失败",
            network,
        )
        ApiResult.Failure(
            exception = network,
            code = network.errorCode,
            kind = ApiErrorKind.BUSINESS,
        )
    } catch (throwable: Throwable) {
        logSafeApiCallFailure(tag, throwable.message.nullIfBlank() ?: "未知错误", throwable)
        ApiResult.Failure(
            exception = TaskFlowNetworkException(
                message = throwable.message.nullIfBlank() ?: "UnknownError",
                cause = throwable,
                userMessage = TaskFlowNetworkUserMessages.UNKNOWN,
            ),
            kind = ApiErrorKind.UNKNOWN,
        )
    }
}

private suspend fun <T> runSafeApiCallBlock(
    tag: String,
    retryPolicy: SafeApiCallRetryPolicy,
    block: suspend () -> T,
): T {
    ensureNetworkAvailableOrThrow()
    val maxAttempts = retryPolicy.maxRetries + 1
    var attempt = 0
    while (true) {
        try {
            return block()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            val shouldRetry = attempt < maxAttempts - 1 && retryPolicy.retryOn(throwable)
            if (!shouldRetry) {
                throw throwable
            }
            attempt++
            logSafeApiCallRetry(
                tag = tag,
                attempt = attempt,
                maxRetries = retryPolicy.maxRetries,
                throwable = throwable,
            )
            delay(retryPolicy.delayMillis)
        }
    }
}

/**
 * 检查网络是否可用，如果不可用则抛出异常
 * 该函数用于确保在进行任务流操作前网络连接是正常的
 *
 * @throws NetworkChecker.unavailableNetworkException 当网络不可用时抛出异常
 */
private fun ensureNetworkAvailableOrThrow() {
    // 检查任务流网络是否已连接
    if (NetworkChecker.isTaskFlowNetworkConnected()) {
        // 如果网络已连接，则直接返回
        return
    }
    // 如果网络未连接，抛出网络不可用异常
    throw NetworkChecker.unavailableNetworkException()
}

private fun logSafeApiCallRetry(
    tag: String,
    attempt: Int,
    maxRetries: Int,
    throwable: Throwable,
) {
    logSafeApiCallFailure(
        tag,
        "网络请求重试 attempt=$attempt/$maxRetries cause=${throwable.javaClass.simpleName}",
        throwable,
    )
}

private fun parseFailure(cause: Throwable): ApiResult.Failure {
    return ApiResult.Failure(
        exception = TaskFlowNetworkException(
            message = "JsonParseException: ${cause.message.nullIfBlank() ?: "parse error"}",
            cause = cause,
            userMessage = TaskFlowNetworkUserMessages.PARSE,
        ),
        kind = ApiErrorKind.PARSE,
    )
}

private fun networkException(
    technical: String,
    userMessage: String,
    cause: Throwable,
): TaskFlowNetworkException {
    return TaskFlowNetworkException(
        message = technical,
        cause = cause,
        userMessage = userMessage,
    )
}
