package com.example.zhttaskflow.core.network

/**
 * 统一 API 结果封装，便于 Domain 层处理成功/失败。
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Failure(val exception: Throwable, val code: Int? = null) : ApiResult<Nothing>()
}
