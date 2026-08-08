package com.example.zhttaskflow.core.network

/**
 * 网络 / 数据访问统一结果封装。
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()

    data class Failure(
        val exception: Throwable,
        val code: Int? = null,
    ) : ApiResult<Nothing>()
}
