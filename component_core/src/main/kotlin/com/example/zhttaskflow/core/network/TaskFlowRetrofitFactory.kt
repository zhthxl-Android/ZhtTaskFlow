package com.example.zhttaskflow.core.network

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit + OkHttp 统一工厂，依赖由调用方通过构造函数传入配置，无 DI 框架。
 */
class TaskFlowRetrofitFactory(
    private val config: TaskFlowNetworkConfig,
) {

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

    fun createRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val gson = GsonBuilder().create()
        return Retrofit.Builder()
            .baseUrl(config.baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun <T> createService(serviceClass: Class<T>, okHttpClient: OkHttpClient): T {
        return createRetrofit(okHttpClient).create(serviceClass)
    }
}
