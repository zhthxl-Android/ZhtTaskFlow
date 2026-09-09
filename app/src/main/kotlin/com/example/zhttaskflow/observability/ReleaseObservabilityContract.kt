package com.example.zhttaskflow.observability

import android.util.Log
import com.example.zhttaskflow.core.observability.LocalLogStore

/**
 * 生产可观测三联（Analytics / Performance / Crash）统一落盘与 SDK 对接契约。
 *
 * 单行日志字段：`channel`、`event`、`pageId`、`actionId`、`params`（与埋点体系一致，便于 ELK / 自研平台解析）。
 */
internal object ReleaseObservabilityContract {

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

    const val SHELL_PAGE_ID: String = "AppShell"

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
        val stackTrace = throwable?.let { stackTraceOf(it) }
        val storeChannel = when (channel) {
            Channel.ANALYTICS -> LocalLogStore.ObservabilityChannel.ANALYTICS
            Channel.PERFORMANCE -> LocalLogStore.ObservabilityChannel.PERFORMANCE
            Channel.CRASH -> LocalLogStore.ObservabilityChannel.CRASH
        }
        LocalLogStore.recordFromObservabilityEmit(
            channel = storeChannel,
            eventOrMetric = eventOrMetric,
            pageId = pageId,
            actionId = actionId,
            params = params,
            stackTrace = stackTrace,
            anomaly = params?.get("anomaly") == "true",
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
        channel: Channel,
        payload: String,
        pageId: String?,
        actionId: String?,
        params: Map<String, String?>?,
        throwable: Throwable?,
    ) {
        when (channel) {
            Channel.ANALYTICS -> {
                // TODO: MyCompanyAnalytics.trackEvent(pageId, actionId, params)
            }
            Channel.PERFORMANCE -> {
                // TODO: MyCompanyApm.reportMetric(pageId, actionId, params)
            }
            Channel.CRASH -> {
                // TODO: Bugly.postException(throwable) / Crashlytics.recordException(throwable)
                // TODO: Bugly / 平台 ANR 由 SDK 初始化时一并开启；本地 [ReleaseCrashReporter.reportAnr] 走同一通道
            }
        }
    }
}
