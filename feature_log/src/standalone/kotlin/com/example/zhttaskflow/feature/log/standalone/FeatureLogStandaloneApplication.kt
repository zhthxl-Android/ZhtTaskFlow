package com.example.zhttaskflow.feature.log.standalone

import android.app.Application
import com.example.zhttaskflow.core.observability.TaskFlowLocalLogStore

/** feature_log 独立运行 Application：初始化本地日志仓。 */
class FeatureLogStandaloneApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        TaskFlowLocalLogStore.init(applicationContext)
    }
}
