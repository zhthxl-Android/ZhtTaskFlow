package com.example.zhttaskflow.core.network

import com.example.zhttaskflow.core.foundation.TaskFlowNetworkException
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
 * 校验 [ApiResponse] 并剥离业务 [data]；失败时抛出 [com.example.zhttaskflow.core.foundation.TaskFlowNetworkException]。
 */
fun <T> unwrapApiResponse(response: ApiResponse<T>): T {
    val code = response.errorCode ?: -1
    if (code != ApiResponse.SUCCESS_CODE) {
        throw TaskFlowNetworkException(
            message = response.errorMsg.nullIfBlank() ?: "errorCode=$code",
            errorCode = code,
            rawErrorMsg = response.errorMsg,
        )
    }
    return response.data ?: throw TaskFlowNetworkException(
        message = response.errorMsg.nullIfBlank()
            ?: "接口未返回 data",
        errorCode = code,
        rawErrorMsg = response.errorMsg,
    )
}
