package com.example.zhttaskflow.base.foundation

/**
 * TaskFlow 业务异常基类。
 */
open class TaskFlowException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * 非法状态异常：数据一致性或业务前置条件不满足时使用。
 */
class TaskFlowIllegalStateException(
    message: String? = null,
    cause: Throwable? = null,
) : TaskFlowException(message, cause)

/**
 * 网络层异常：由 [com.example.zhttaskflow.core.network.safeApiCall] 等统一映射。
 */
class TaskFlowNetworkException(
    message: String? = null,
    cause: Throwable? = null,
) : TaskFlowException(message, cause)
