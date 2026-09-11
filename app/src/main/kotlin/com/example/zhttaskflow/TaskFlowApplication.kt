package com.example.zhttaskflow

import android.app.Application
import com.example.zhttaskflow.base.exception.AppCoroutineExceptionHandler
import com.example.zhttaskflow.base.exception.DebugCrashReporter
import com.example.zhttaskflow.core.observability.LocalLogStore
import com.example.zhttaskflow.core.util.bindNetworkDiagnostics
import com.example.zhttaskflow.core.util.isDebugLoggingEnabled
import com.example.zhttaskflow.exception.ReleaseCrashMonitoring
import com.example.zhttaskflow.exception.ReleaseCrashReporter

/** 壳 Application：全局同步网络/数据层 Debug 诊断开关，并初始化自研本地可观测日志仓。 */
class TaskFlowApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        //运行环境初始化（Debug/Release）
        bindNetworkDiagnostics(applicationContext)
        //本地可观测日志仓初始化
        LocalLogStore.init(applicationContext)
        //全局异常捕获
        val crashReporter = if (isDebugLoggingEnabled()) {
            DebugCrashReporter
        } else {
            ReleaseCrashReporter
        }
        //全局协程异常捕获
        AppCoroutineExceptionHandler.install(crashReporter)
        ReleaseCrashMonitoring.install(this)
    }
}
