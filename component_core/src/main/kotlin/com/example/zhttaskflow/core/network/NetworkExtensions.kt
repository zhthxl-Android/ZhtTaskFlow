package com.example.zhttaskflow.core.network

import com.example.zhttaskflow.base.foundation.TaskFlowIllegalStateException
import com.example.zhttaskflow.base.foundation.TaskFlowLogger
import com.example.zhttaskflow.base.foundation.TaskFlowNetworkException
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 在 IO 线程执行挂起块，并包装为 [ApiResult]；记录日志并区分网络 / 业务 / 解析异常。
 *
 * @param tag 日志 Tag
 * @param block 网络或 IO 挂起调用
 */
suspend fun <T> safeApiCall(
    tag: String = "safeApiCall",
    block: suspend () -> T,
): ApiResult<T> = withContext(Dispatchers.IO) {
    try {
        ApiResult.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (httpException: HttpException) {
        val code = httpException.code()
        val message = httpException.message() ?: "HTTP $code"
        TaskFlowLogger.e(tag, message, httpException)
        val kind = if (code in 400..499) {
            ApiErrorKind.BUSINESS
        } else {
            ApiErrorKind.NETWORK
        }
        ApiResult.Failure(
            exception = TaskFlowNetworkException(message = message, cause = httpException),
            code = code,
            kind = kind,
        )
    } catch (parseError: JsonSyntaxException) {
        TaskFlowLogger.e(tag, "JSON 解析失败", parseError)
        ApiResult.Failure(
            exception = TaskFlowIllegalStateException(
                message = "数据解析失败",
                cause = parseError,
            ),
            kind = ApiErrorKind.PARSE,
        )
    } catch (parseIo: JsonIOException) {
        TaskFlowLogger.e(tag, "JSON IO 异常", parseIo)
        ApiResult.Failure(
            exception = TaskFlowIllegalStateException(
                message = "数据解析失败",
                cause = parseIo,
            ),
            kind = ApiErrorKind.PARSE,
        )
    } catch (io: IOException) {
        TaskFlowLogger.e(tag, "网络 IO 异常", io)
        ApiResult.Failure(
            exception = TaskFlowNetworkException(
                message = io.message ?: "网络请求失败",
                cause = io,
            ),
            kind = when (io) {
                is UnknownHostException, is SocketTimeoutException -> ApiErrorKind.NETWORK
                else -> ApiErrorKind.NETWORK
            },
        )
    } catch (throwable: Throwable) {
        TaskFlowLogger.e(tag, throwable.message ?: "未知错误", throwable)
        ApiResult.Failure(
            exception = TaskFlowNetworkException(
                message = "请求失败",
                cause = throwable,
            ),
            kind = ApiErrorKind.UNKNOWN,
        )
    }
}
