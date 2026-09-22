package com.example.zhttaskflow.base.observability

/**
 * 可观测契约常量与 Release 风格落盘（全工程唯一常量源）。
 *
 * Debug 面板切换 [com.example.zhttaskflow.core.debug.DeveloperTools.ObservabilityBackend.RELEASE_LOCAL] 时走 [emit]；
 * 生产壳层经 `app` 模块 [com.example.zhttaskflow.observability.ReleaseObservabilityContract.emit] 落盘并对接 SDK。
 */
object LocalObservabilityEmitter {

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
        ObservabilityEmitPipeline.emit(
            channel = channel,
            eventOrMetric = eventOrMetric,
            pageId = pageId,
            actionId = actionId,
            params = params,
            throwable = throwable,
        )
    }
}
