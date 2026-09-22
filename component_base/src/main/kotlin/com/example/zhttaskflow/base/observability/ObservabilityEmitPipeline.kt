package com.example.zhttaskflow.base.observability

import android.util.Log
import com.example.zhttaskflow.core.observability.LocalLogStore

/**
 * 可观测三联统一落盘；落盘后回调 [ObservabilityPlatformHook]。
 */
object ObservabilityEmitPipeline {

    @Volatile
    var platformHook: ObservabilityPlatformHook = ObservabilityPlatformHook.NoOp

    fun installPlatformHook(hook: ObservabilityPlatformHook) {
        platformHook = hook
    }

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
        try {
            platformHook.onObservabilityEvent(
                channel = channel,
                payload = payload,
                pageId = pageId,
                actionId = actionId,
                params = params,
                throwable = throwable,
            )
        } catch (t: Throwable) {
            Log.e(
                LocalObservabilityEmitter.LOG_TAG,
                "platform hook execution failed",
                t,
            )
        }
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
}
