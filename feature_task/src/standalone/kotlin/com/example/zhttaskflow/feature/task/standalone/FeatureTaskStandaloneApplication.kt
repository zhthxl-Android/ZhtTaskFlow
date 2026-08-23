package com.example.zhttaskflow.feature.task.standalone

import android.app.Application
import com.example.zhttaskflow.core.network.bindTaskFlowNetworkDiagnostics

class FeatureTaskStandaloneApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        bindTaskFlowNetworkDiagnostics(applicationContext)
    }
}
