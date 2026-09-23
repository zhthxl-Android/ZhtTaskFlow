package com.example.zhttaskflow.bootstrap

import android.app.Application
import com.example.zhttaskflow.base.exception.AppCoroutineExceptionHandler
import com.example.zhttaskflow.base.exception.DebugCrashReporter
import com.example.zhttaskflow.base.exception.ReleaseCrashReporter
import com.example.zhttaskflow.core.observability.LocalLogStore
import com.example.zhttaskflow.core.util.isDebugLoggingEnabled
import com.example.zhttaskflow.observability.AppObservabilityAssembly

/**
 * 应用壳启动装配：可观测日志仓、平台 Hook 与全局协程异常处理。
 */
object AppBootstrap {

    /**
     * 在 [Application.onCreate] 中、运行环境初始化绑定之后调用。
     */
    fun installObservability(application: Application) {
        // 日志仓初始化
        LocalLogStore.init(application.applicationContext)
        // 第三方sdk钩子回调初始化
        AppObservabilityAssembly.install()
        // 全局协程异常处理初始化
        val crashReporter = if (isDebugLoggingEnabled()) {
            DebugCrashReporter
        } else {
            ReleaseCrashReporter
        }
        AppCoroutineExceptionHandler.install(crashReporter)
    }
}
