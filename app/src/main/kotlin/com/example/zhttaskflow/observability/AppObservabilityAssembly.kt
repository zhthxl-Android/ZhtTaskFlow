package com.example.zhttaskflow.observability

import com.example.zhttaskflow.base.observability.LocalObservabilityEmitter
import com.example.zhttaskflow.base.observability.ObservabilityEmitPipeline
import com.example.zhttaskflow.base.observability.ObservabilityPlatformHook

/**
 * 应用壳可观测扩展装配：注册第三方 SDK 对接 [ObservabilityPlatformHook]。
 */
object AppObservabilityAssembly {

    fun install() {
        ObservabilityEmitPipeline.installPlatformHook(AppObservabilityPlatformHook)
    }

    private object AppObservabilityPlatformHook : ObservabilityPlatformHook {
        override fun onObservabilityEvent(
            channel: LocalObservabilityEmitter.Channel,
            payload: String,
            pageId: String?,
            actionId: String?,
            params: Map<String, String?>?,
            throwable: Throwable?,
        ) {
            when (channel) {
                LocalObservabilityEmitter.Channel.ANALYTICS -> {
                    // TODO: MyCompanyAnalytics.trackEvent(pageId, actionId, params)
                }
                LocalObservabilityEmitter.Channel.PERFORMANCE -> {
                    // TODO: MyCompanyApm.reportMetric(pageId, actionId, params)
                }
                LocalObservabilityEmitter.Channel.CRASH -> {
                    // TODO: Bugly.postException(throwable) / Crashlytics.recordException(throwable)
                }
            }
        }
    }
}
