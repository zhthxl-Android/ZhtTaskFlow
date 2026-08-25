package com.example.zhttaskflow.feature.home.standalone

import android.app.Application
import com.example.zhttaskflow.core.util.bindTaskFlowNetworkDiagnostics

class FeatureHomeStandaloneApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        bindTaskFlowNetworkDiagnostics(applicationContext)
    }
}
