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
 *
 * 调用顺序与 [com.example.zhttaskflow.TaskFlowApplication] 改造前一致，仅做入口收敛。
 */
object AppBootstrap {

    /**
     * 在 [Application.onCreate] 中、网络诊断绑定之后调用。
     */
    fun installObservability(application: Application) {
        LocalLogStore.init(application.applicationContext)
        AppObservabilityAssembly.install()
        val crashReporter = if (isDebugLoggingEnabled()) {
            DebugCrashReporter
        } else {
            ReleaseCrashReporter
        }
        AppCoroutineExceptionHandler.install(crashReporter)
    }
}
