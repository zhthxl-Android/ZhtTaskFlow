package com.example.zhttaskflow.observability

import android.util.Log
import com.example.zhttaskflow.base.observability.LocalObservabilityEmitter
import com.example.zhttaskflow.core.observability.LocalLogStore

/**
 * 生产可观测三联（Analytics / Performance / Crash）统一落盘与 SDK 对接契约。
 *
 * 字段常量见 [LocalObservabilityEmitter]；本对象负责 Release 落盘、异常通道与第三方 SDK 扩展点。
 */
internal object ReleaseObservabilityContract {

    fun emit(
        channel: LocalObservabilityEmitter.Channel,
        eventOrMetric: String,
        pageId: String?,
        actionId: String?,
        params: Map<String, String?>? = null,
        throwable: Throwable? = null,
    ) {
        val payload = buildPayload(
            channel = channel,
            eventOrMetric = eventOrMetric,
            pageId = pageId,
            actionId = actionId,
            params = params,
        )
        when (channel) {
            LocalObservabilityEmitter.Channel.CRASH -> Log.e(LocalObservabilityEmitter.LOG_TAG, payload, throwable)
            else -> Log.i(LocalObservabilityEmitter.LOG_TAG, payload)
        }
        val stackTrace = throwable?.let { stackTraceOf(it) }
        val storeChannel = when (channel) {
            LocalObservabilityEmitter.Channel.ANALYTICS -> LocalLogStore.ObservabilityChannel.ANALYTICS
            LocalObservabilityEmitter.Channel.PERFORMANCE -> LocalLogStore.ObservabilityChannel.PERFORMANCE
            LocalObservabilityEmitter.Channel.CRASH -> LocalLogStore.ObservabilityChannel.CRASH
        }
        LocalLogStore.recordFromObservabilityEmit(
            channel = storeChannel,
            eventOrMetric = eventOrMetric,
            pageId = pageId,
            actionId = actionId,
            params = params,
            stackTrace = stackTrace,
            anomaly = params?.get("anomaly") == "true" || channel == LocalObservabilityEmitter.Channel.CRASH,
        )
        dispatchToCompanyPlatform(
            channel = channel,
            payload = payload,
            pageId = pageId,
            actionId = actionId,
            params = params,
            throwable = throwable,
        )
    }

    private fun buildPayload(
        channel: LocalObservabilityEmitter.Channel,
        eventOrMetric: String,
        pageId: String?,
        actionId: String?,
        params: Map<String, String?>?,
    ): String {
        return buildString {
            append("channel=${channel.name.lowercase()}")
            append(" event=$eventOrMetric")
            if (!pageId.isNullOrBlank()) {
                append(" pageId=$pageId")
            }
            if (!actionId.isNullOrBlank()) {
                append(" actionId=$actionId")
            }
            val snapshot = formatParams(params)
            if (snapshot.isNotBlank()) {
                append(" params=$snapshot")
            }
        }
    }

    fun formatParams(params: Map<String, String?>?): String {
        return params
            ?.entries
            ?.mapNotNull { (key, value) -> value?.let { safe -> "$key=$safe" } }
            ?.joinToString(separator = ",")
            .orEmpty()
    }

    private fun stackTraceOf(throwable: Throwable): String {
        return buildString {
            append(throwable::class.java.name)
            append(": ")
            append(throwable.message.orEmpty())
            append('\n')
            append(throwable.stackTraceToString())
        }
    }

    /**
     * 公司埋点 / APM / 崩溃平台统一接入点（友盟、自研、Bugly 等）。
     */
    private fun dispatchToCompanyPlatform(
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
                // TODO: Bugly / 平台 ANR 由 SDK 初始化时一并开启；本地 [ReleaseCrashReporter.reportAnr] 走同一通道
            }
        }
    }
}
