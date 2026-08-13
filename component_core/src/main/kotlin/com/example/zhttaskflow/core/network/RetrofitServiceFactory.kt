package com.example.zhttaskflow.core.network

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit + OkHttp 统一工厂，对外提供创建 Service 的标准入口。
 *
 * 第三方类型仅在本模块内使用，业务层通过本类获取 API Service 接口实例。
 */
class RetrofitServiceFactory(
    private val config: TaskFlowNetworkConfig,
) {

    init {
        require(config.baseUrl.endsWith("/")) {
            "Retrofit baseUrl 必须以 / 结尾: ${config.baseUrl}"
        }
    }

    /**
     * 创建配置完成的 [OkHttpClient]。
     * BODY 日志仅当 [TaskFlowNetworkConfig.enableLogging] 为 true 时启用（Release 须保持 false）。
     */
    fun createOkHttpClient(
        extraInterceptors: List<okhttp3.Interceptor> = emptyList(),
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(TaskFlowHeaderInterceptor { config.defaultHeaders })
            .addInterceptor(TaskFlowResponseInterceptor())
        extraInterceptors.forEach { builder.addInterceptor(it) }
        if (config.enableLogging) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                },
            )
        }
        return builder.build()
    }

    /**
     * 基于已配置的 [OkHttpClient] 创建 [Retrofit]。
     */
    fun createRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val gson = GsonBuilder().create()
        return Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * 一步创建 Retrofit API Service。
     */
    fun <T> createService(serviceClass: Class<T>, okHttpClient: OkHttpClient): T {
        return createRetrofit(okHttpClient).create(serviceClass)
    }
}
