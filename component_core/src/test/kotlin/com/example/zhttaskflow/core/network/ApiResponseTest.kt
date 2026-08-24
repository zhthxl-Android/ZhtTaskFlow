package com.example.zhttaskflow.core.network

import com.example.zhttaskflow.core.foundation.TaskFlowNetworkException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiResponseTest {

    @Test
    fun unwrap_success_returns_data() {
        val response = ApiResponse(data = "payload", errorCode = 0, errorMsg = "")
        assertEquals("payload", unwrapApiResponse(response))
    }

    @Test
    fun unwrap_business_error_throws_network_exception() {
        val response = ApiResponse<String?>(data = null, errorCode = -1, errorMsg = "业务失败")
        try {
            unwrapApiResponse(response)
            error("应抛出异常")
        } catch (e: TaskFlowNetworkException) {
            assertEquals(-1, e.errorCode)
            assertEquals("业务失败", e.message)
        }
    }

    @Test
    fun safeApiCall_unwraps_api_response() = kotlinx.coroutines.test.runTest {
        val result = safeApiCallResponse {
            ApiResponse(data = 42, errorCode = 0, errorMsg = "")
        }
        assertTrue(result is ApiResult.Success)
        assertEquals(42, (result as ApiResult.Success).data)
    }
}
