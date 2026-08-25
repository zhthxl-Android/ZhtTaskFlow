package com.example.zhttaskflow

import android.app.Application
import com.example.zhttaskflow.core.util.bindTaskFlowNetworkDiagnostics

/** 壳 Application：全局同步网络/数据层 Debug 诊断开关。 */
class TaskFlowApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        bindTaskFlowNetworkDiagnostics(applicationContext)
    }
}
