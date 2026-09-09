package com.example.zhttaskflow.base.observability

import android.util.Log
import com.example.zhttaskflow.core.observability.LocalLogStore

/**
 * Release 风格可观测事件落盘（Debug 面板切换 [com.example.zhttaskflow.core.debug.DeveloperTools.ObservabilityBackend.RELEASE_LOCAL] 时使用）。
 *
 * 字段与 `app` 模块 [com.example.zhttaskflow.observability.ReleaseObservabilityContract] 对齐，避免 feature 依赖 app。
 */
internal object LocalObservabilityEmitter {

    const val LOG_TAG: String = "TaskFlow/Observability"

    const val ACTION_PAGE_ENTER: String = "page_enter"
    const val ACTION_PAGE_LEAVE: String = "page_leave"
    const val ACTION_PAGE_ARGS_CHANGE: String = "page_args_change"
    const val ACTION_APP_UNCAUGHT_CRASH: String = "app_uncaught_crash"
    const val ACTION_APP_ANR: String = "app_anr"

    const val METRIC_FIRST_FRAME: String = "first_frame"
    const val METRIC_SCROLL_FPS: String = "scroll_fps"
    const val METRIC_PAGE_DWELL: String = "page_dwell"

    const val EVENT_CRASH: String = "crash"
    const val EVENT_ANR: String = "anr"

    enum class Channel {
        ANALYTICS,
        PERFORMANCE,
        CRASH,
    }

    fun emit(
        channel: Channel,
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
            Channel.CRASH -> Log.e(LOG_TAG, payload, throwable)
            else -> Log.i(LOG_TAG, payload)
        }
        val storeChannel = when (channel) {
            Channel.ANALYTICS -> LocalLogStore.ObservabilityChannel.ANALYTICS
            Channel.PERFORMANCE -> LocalLogStore.ObservabilityChannel.PERFORMANCE
            Channel.CRASH -> LocalLogStore.ObservabilityChannel.CRASH
        }
        val stackTrace = throwable?.let { stackTraceOf(it) }
        LocalLogStore.recordFromObservabilityEmit(
            channel = storeChannel,
            eventOrMetric = eventOrMetric,
            pageId = pageId,
            actionId = actionId,
            params = params,
            stackTrace = stackTrace,
            anomaly = params?.get("anomaly") == "true" || channel == Channel.CRASH,
        )
    }

    private fun buildPayload(
        channel: Channel,
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

    private fun formatParams(params: Map<String, String?>?): String {
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
