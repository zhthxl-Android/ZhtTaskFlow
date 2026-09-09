package com.example.zhttaskflow.feature.article.standalone

import android.app.Application
import com.example.zhttaskflow.core.util.bindNetworkDiagnostics

class FeatureArticleStandaloneApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        bindNetworkDiagnostics(applicationContext)
    }
}
