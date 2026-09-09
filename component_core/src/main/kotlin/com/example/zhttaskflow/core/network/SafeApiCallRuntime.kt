package com.example.zhttaskflow.core.network

import android.content.Context

/**
 * [safeApiCall] 运行时上下文：用于 [NetworkChecker] 前置检查，由宿主 [com.example.zhttaskflow.core.util.bindNetworkDiagnostics] 绑定。
 */
internal object SafeApiCallRuntime {

    @Volatile
    private var applicationContext: Context? = null

    fun bindContext(context: Context) {
        applicationContext = context.applicationContext
    }

    fun applicationContextOrNull(): Context? = applicationContext
}
