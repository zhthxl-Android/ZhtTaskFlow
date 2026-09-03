package com.example.zhttaskflow

import android.app.Application
import com.example.zhttaskflow.base.exception.TaskFlowCoroutineExceptionHandler
import com.example.zhttaskflow.base.exception.TaskFlowDebugCrashReporter
import com.example.zhttaskflow.core.util.bindTaskFlowNetworkDiagnostics
import com.example.zhttaskflow.core.util.isTaskFlowDebugLoggingEnabled
import com.example.zhttaskflow.exception.ReleaseTaskFlowCrashMonitoring
import com.example.zhttaskflow.exception.ReleaseTaskFlowCrashReporter
import com.example.zhttaskflow.observability.TaskFlowLocalLogStore

/** 壳 Application：全局同步网络/数据层 Debug 诊断开关，并初始化自研本地可观测日志仓。 */
class TaskFlowApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        bindTaskFlowNetworkDiagnostics(applicationContext)
        TaskFlowLocalLogStore.init(applicationContext)
        val crashReporter = if (isTaskFlowDebugLoggingEnabled()) {
            TaskFlowDebugCrashReporter
        } else {
            ReleaseTaskFlowCrashReporter
        }
        TaskFlowCoroutineExceptionHandler.install(crashReporter)
        ReleaseTaskFlowCrashMonitoring.install(this)
    }
}
