package com.example.zhttaskflow.feature.task.standalone

import android.app.Application
import com.example.zhttaskflow.core.util.bindNetworkDiagnostics

class FeatureTaskStandaloneApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        bindNetworkDiagnostics(applicationContext)
    }
}
