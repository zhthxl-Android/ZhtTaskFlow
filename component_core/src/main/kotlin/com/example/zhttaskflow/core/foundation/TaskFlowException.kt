package com.example.zhttaskflow.core.foundation

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
 * 网络层异常：由 [com.example.zhttaskflow.core.network.safeApiCall]、[unwrapApiResponse] 等统一映射。
 *
 * @param message 技术详情，供日志与排查使用
 * @param userMessage 面向用户的友好文案，供 UI 展示
 * @param errorCode 服务端业务码（可选）
 * @param rawErrorMsg 服务端原始 errorMsg（可选）
 */
class TaskFlowNetworkException(
    message: String? = null,
    cause: Throwable? = null,
    val errorCode: Int? = null,
    val rawErrorMsg: String? = null,
    val userMessage: String? = null,
) : TaskFlowException(message, cause)

/**
 * UI 展示用友好文案：优先 [TaskFlowNetworkException.userMessage]，禁止把技术 [message] 直接暴露给用户。
 */
fun Throwable.userDisplayMessage(fallback: String): String {
    return when (this) {
        is TaskFlowNetworkException -> userMessage?.takeIf { it.isNotBlank() } ?: fallback
        else -> fallback
    }
}
