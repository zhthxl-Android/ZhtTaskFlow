package com.example.zhttaskflow.core.network

/**
 * 网络请求统一结果类型。
 */
enum class ApiErrorKind {
    /** 网络不可用、超时、HTTP 层失败等 */
    NETWORK,

    /** 服务端业务码 / HTTP 4xx 业务语义 */
    BUSINESS,

    /** JSON 解析或反序列化失败 */
    PARSE,

    /** 未分类错误 */
    UNKNOWN,
}

/**
 * 网络 / 远程数据访问统一结果封装，覆盖成功与多类失败场景。
 */
sealed class ApiResult<out T> {

    /** 成功结果 */
    data class Success<T>(val data: T) : ApiResult<T>()

    /**
     * 失败结果。
     *
     * @param exception 可向上抛出的领域/基础异常
     * @param code 业务或 HTTP 状态码（可选）
     * @param kind 失败分类，便于业务层分支处理
     */
    data class Failure(
        val exception: Throwable,
        val code: Int? = null,
        val kind: ApiErrorKind = ApiErrorKind.UNKNOWN,
    ) : ApiResult<Nothing>()
}
