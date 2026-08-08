package com.example.zhttaskflow.core.network

import com.example.zhttaskflow.base.foundation.TaskFlowNetworkException
import kotlinx.coroutines.CancellationException

/**
 * 将挂起网络调用包装为 [ApiResult]，并映射为 [TaskFlowNetworkException]。
 */
suspend fun <T> safeApiCall(
    block: suspend () -> T,
): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        ApiResult.Failure(
            exception = TaskFlowNetworkException(
                message = throwable.message,
                cause = throwable,
            ),
        )
    }
}
