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
 * Retrofit API 统一工厂：业务层获取网络 Service 的 **唯一推荐入口**（与 [com.example.zhttaskflow.core.persistence.room.TaskFlowRoomTemplate] 范式对齐）。
 *
 * ## 防腐约定
 * - [OkHttpClient.Builder]、[Retrofit.Builder] 等原生 API **仅在本类内部** 使用；
 * - 业务 Feature 仅声明 API 接口注解，通过 [createApi] 获取实例；
 * - 请求执行与异常兜底使用 [safeApiCall]。
 *
 * ## 配置规则
 * - 全局共享 [OkHttpClient]：超时、通用拦截器、日志策略一次配置；
 * - BODY 级日志由宿主 [Context.isAppDebuggable] 决定，Release 安装包自动关闭；
 * - 超时等数值见 [TaskFlowNetworkDefaults]。
 *
 * ## 扩展方式
 * - 额外拦截器：[createApi] 的 `extraInterceptors`；
 * - 额外请求头：`defaultHeaders`（非空时派生独立 Client）。
 */
object RetrofitServiceFactory {

    private var sharedOkHttpClient: OkHttpClient? = null

    private val retrofitCache = ConcurrentHashMap<String, Retrofit>()

    /**
     * 创建 Retrofit API Service 实例。
     *
     * @param context 用于判断宿主是否 debuggable，建议 [Context.getApplicationContext]
     * @param baseUrl 根地址，必须以 `/` 结尾
     * @param serviceClass 带 Retrofit 注解的 API 接口 Class
     * @param extraInterceptors 可选额外应用拦截器
     * @param defaultHeaders 可选 per-api 默认请求头
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
