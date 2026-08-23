package com.example.zhttaskflow.core.network

import android.content.Context
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Retrofit API 统一工厂：封装 OkHttp / Retrofit 构建，业务层获取网络 Service 的唯一推荐入口。
 *
 * **调用方式**： [createApi] 传入 [Context]、baseUrl 与 API 接口 Class；
 * 请求执行与异常兜底使用 [safeApiCall]（在 IO 线程挂起调用）。
 *
 * **线程约束**：工厂本身无阻塞；网络请求须在协程中通过 Retrofit 挂起函数或 [safeApiCall] 调用，禁止主线程同步请求。
 */
object RetrofitServiceFactory {

    private var sharedOkHttpClient: OkHttpClient? = null

    private val retrofitCache = ConcurrentHashMap<String, Retrofit>()

    /**
     * 创建 Retrofit API Service 实例。
     */
    fun <T> createApi(
        context: Context,
        baseUrl: String,
        serviceClass: Class<T>,
        extraInterceptors: List<Interceptor> = emptyList(),
        defaultHeaders: Map<String, String> = emptyMap(),
    ): T {
        require(baseUrl.endsWith("/")) {
            "Retrofit baseUrl 必须以 / 结尾: $baseUrl"
        }
        val appContext = context.applicationContext
        val client = obtainOkHttpClient(appContext, extraInterceptors, defaultHeaders)
        val retrofit = obtainRetrofit(baseUrl, client)
        return retrofit.create(serviceClass)
    }

    private fun obtainOkHttpClient(
        context: Context,
        extraInterceptors: List<Interceptor>,
        defaultHeaders: Map<String, String>,
    ): OkHttpClient {
        if (extraInterceptors.isEmpty() && defaultHeaders.isEmpty()) {
            return sharedOkHttpClient(context)
        }
        val builder = sharedOkHttpClient(context).newBuilder()
        if (defaultHeaders.isNotEmpty()) {
            builder.addInterceptor(TaskFlowHeaderInterceptor { defaultHeaders })
        }
        extraInterceptors.forEach { interceptor ->
            builder.addInterceptor(interceptor)
        }
        return builder.build()
    }

    private fun sharedOkHttpClient(context: Context): OkHttpClient {
        val cached = sharedOkHttpClient
        if (cached != null) {
            return cached
        }
        return buildSharedOkHttpClient(context).also { sharedOkHttpClient = it }
    }

    private fun obtainRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        val cacheKey = "$baseUrl@${client.hashCode()}"
        return retrofitCache.getOrPut(cacheKey) {
            val gson = GsonBuilder().create()
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
    }

    private fun buildSharedOkHttpClient(context: Context): OkHttpClient {
        TaskFlowNetworkDiagnostics.syncFrom(context)
        val builder = OkHttpClient.Builder()
            .connectTimeout(
                TaskFlowNetworkDefaults.CONNECT_TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
            .readTimeout(
                TaskFlowNetworkDefaults.READ_TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
            .writeTimeout(
                TaskFlowNetworkDefaults.WRITE_TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
            .addInterceptor(TaskFlowHeaderInterceptor { emptyMap() })
            .addInterceptor(TaskFlowResponseInterceptor())
        if (context.isAppDebuggable()) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                },
            )
        }
        return builder.build()
    }
}
