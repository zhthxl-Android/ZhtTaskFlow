package com.example.zhttaskflow.core.network

import com.example.zhttaskflow.core.foundation.NetworkException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

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
        } catch (e: NetworkException) {
            assertEquals(-1, e.errorCode)
            assertEquals("业务失败", e.userMessage)
            assertTrue(e.message?.contains("errorCode=-1") == true)
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

    @Test
    fun safeApiCall_api_response_via_safeApiCallResponse() = kotlinx.coroutines.test.runTest {
        val result = safeApiCallResponse {
            ApiResponse(data = "ok", errorCode = 0, errorMsg = "")
        }
        assertTrue(result is ApiResult.Success)
        assertEquals("ok", (result as ApiResult.Success).data)
    }

    @Test
    fun safeApiCall_maps_socket_timeout_user_message() = kotlinx.coroutines.test.runTest {
        val result = safeApiCall<Unit> {
            throw SocketTimeoutException("timeout")
        }
        assertTrue(result is ApiResult.Failure)
        val ex = (result as ApiResult.Failure).exception as NetworkException
        assertEquals(NetworkUserMessages.NETWORK_TIMEOUT, ex.userMessage)
        assertTrue(ex.message?.contains("SocketTimeoutException") == true)
    }

    @Test
    fun safeApiCall_retries_once_on_socket_timeout() = kotlinx.coroutines.test.runTest {
        var attempts = 0
        val result = safeApiCall<Unit> {
            attempts++
            if (attempts == 1) {
                throw SocketTimeoutException("timeout")
            }
        }
        assertTrue(result is ApiResult.Success)
        assertEquals(2, attempts)
    }

    @Test
    fun safeApiCall_no_retry_on_business_exception() = kotlinx.coroutines.test.runTest {
        var attempts = 0
        val result = safeApiCall<Unit> {
            attempts++
            throw NetworkException(
                message = "errorCode=-1",
                errorCode = -1,
                userMessage = "业务失败",
            )
        }
        assertTrue(result is ApiResult.Failure)
        assertEquals(1, attempts)
    }
}
