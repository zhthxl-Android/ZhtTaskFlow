package com.example.zhttaskflow.core.network

import com.example.zhttaskflow.core.foundation.NetworkException
import com.example.zhttaskflow.core.util.nullIfBlank
import com.google.gson.annotations.SerializedName

/**
 * 玩 Android 等接口通用外层响应：`data` + `errorCode` + `errorMsg`。
 *
 * 成功约定：[errorCode] == [SUCCESS_CODE]（0）；其余为业务失败。
 */
data class ApiResponse<T>(
    @SerializedName("data")
    val data: T?,
    @SerializedName("errorCode")
    val errorCode: Int?,
    @SerializedName("errorMsg")
    val errorMsg: String?,
) {

    companion object {
        const val SUCCESS_CODE: Int = 0
    }

    val isSuccess: Boolean = errorCode == SUCCESS_CODE
}

/**
 * 校验 [ApiResponse] 并剥离业务 [data]；失败时抛出 [com.example.zhttaskflow.core.foundation.NetworkException]。
 */
fun <T> unwrapApiResponse(response: ApiResponse<T>): T {
    val code = response.errorCode ?: -1
    if (code != ApiResponse.SUCCESS_CODE) {
        throw businessApiException(
            code = code,
            rawErrorMsg = response.errorMsg,
            technicalDetail = "服务端业务失败 errorCode=$code",
        )
    }
    return response.data ?: throw businessApiException(
        code = code,
        rawErrorMsg = response.errorMsg,
        technicalDetail = "接口成功但 data 为空 errorCode=$code",
    )
}

internal fun businessApiException(
    code: Int,
    rawErrorMsg: String?,
    technicalDetail: String,
): NetworkException {
    val userMessage = rawErrorMsg.nullIfBlank() ?: NetworkUserMessages.BUSINESS_FALLBACK
    return NetworkException(
        message = "$technicalDetail rawErrorMsg=${rawErrorMsg.orEmpty()}",
        errorCode = code,
        rawErrorMsg = rawErrorMsg,
        userMessage = userMessage,
    )
}
