package com.example.zhttaskflow.base.foundation

/**
 * 全局业务/技术异常根类型，便于统一捕获与展示。
 */
open class TaskFlowException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

/** 网络相关异常 */
class TaskFlowNetworkException(
    message: String? = null,
    cause: Throwable? = null,
    val code: Int? = null,
) : TaskFlowException(message, cause)

/** 本地 IO / 数据库异常 */
class TaskFlowIoException(
    message: String? = null,
    cause: Throwable? = null,
) : TaskFlowException(message, cause)

/** 参数或状态非法 */
class TaskFlowIllegalStateException(
    message: String? = null,
    cause: Throwable? = null,
) : TaskFlowException(message, cause)
