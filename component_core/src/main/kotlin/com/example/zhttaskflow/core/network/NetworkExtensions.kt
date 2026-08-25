package com.example.zhttaskflow.core.network

import com.example.zhttaskflow.core.foundation.TaskFlowNetworkException
import com.example.zhttaskflow.core.util.nullIfBlank
import com.google.gson.JsonIOException
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * 在 IO 线程执行挂起块，并包装为 [ApiResult]；区分网络 / 业务 / 解析异常。
 *
 * **日志**：Release 不打印 Error；Debug 安装包输出一条 Debug 级详细日志（含堆栈）。业务 Error 由 ViewModel [launchTask] 统一记录。
 *
 * 当 [block] 返回 [ApiResponse] 时，请使用 [safeApiCallResponse] 自动拆包，无需手动 [unwrapApiResponse]。
 *
 * @param tag 日志 Tag
 * @param block 网络或 IO 挂起调用
 */
suspend fun <T> safeApiCall(
    tag: String = "safeApiCall",
    block: suspend () -> T,
): ApiResult<T> = executeSafeApiCall(tag, block)

/**
 * [block] 返回标准外层 [ApiResponse] 时自动拆包：成功 [ApiResult] 直接承载业务 DTO。
 */
suspend fun <T> safeApiCallResponse(
    tag: String = "safeApiCall",
    block: suspend () -> ApiResponse<T>,
): ApiResult<T> = executeSafeApiCall(tag) {
    unwrapApiResponse(block())
}

internal suspend fun <T> executeSafeApiCall(
    tag: String,
    block: suspend () -> T,
): ApiResult<T> = withContext(Dispatchers.IO) {
    try {
        ApiResult.Success(block())
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
