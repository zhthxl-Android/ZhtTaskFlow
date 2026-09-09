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
        bindNetworkDiagnostics(applicationContext)
        LocalLogStore.init(applicationContext)
        val crashReporter = if (isDebugLoggingEnabled()) {
            DebugCrashReporter
        } else {
            ReleaseCrashReporter
        }
        AppCoroutineExceptionHandler.install(crashReporter)
        ReleaseCrashMonitoring.install(this)
    }
}
