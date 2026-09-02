package com.example.zhttaskflow

import android.app.Application
import com.example.zhttaskflow.core.util.bindTaskFlowNetworkDiagnostics
import com.example.zhttaskflow.exception.ReleaseTaskFlowCrashMonitoring

/** 壳 Application：全局同步网络/数据层 Debug 诊断开关。 */
class TaskFlowApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        bindTaskFlowNetworkDiagnostics(applicationContext)
        ReleaseTaskFlowCrashMonitoring.install(this)
    }
}
