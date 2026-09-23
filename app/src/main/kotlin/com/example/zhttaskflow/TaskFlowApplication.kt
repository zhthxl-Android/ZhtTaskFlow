package com.example.zhttaskflow

import android.app.Application
import com.example.zhttaskflow.bootstrap.AppBootstrap
import com.example.zhttaskflow.core.util.initRuntimeDiagnostics
import com.example.zhttaskflow.base.exception.ReleaseAnrMonitor

/** 壳 Application：全局同步网络/数据层 Debug 诊断开关，并初始化自研本地可观测日志仓。 */
class TaskFlowApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        //运行环境初始化（Debug/Release）
        initRuntimeDiagnostics(applicationContext)
        AppBootstrap.installObservability(this)
        //全局 ANR 监控
        ReleaseAnrMonitor.install(this)
    }
}
