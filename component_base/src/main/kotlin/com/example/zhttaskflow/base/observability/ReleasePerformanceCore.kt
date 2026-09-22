package com.example.zhttaskflow.base.observability

/**
 * Release 风格性能指标共用逻辑：阈值判定与契约字段映射。
 */
internal object ReleasePerformanceCore {

    private const val FIRST_FRAME_SLOW_THRESHOLD_MS: Long = 700L
    private const val SCROLL_FPS_MIN_THRESHOLD: Float = 45f

    internal fun interface PerformanceEmit {
        fun emit(
            eventOrMetric: String,
            pageId: String,
            actionId: String,
            params: Map<String, String>,
        )
    }

    fun onFirstFrameRendered(
        pageId: String,
        durationMs: Long,
        emit: PerformanceEmit,
    ) {
        val anomaly = durationMs > FIRST_FRAME_SLOW_THRESHOLD_MS
        emit.emit(
            eventOrMetric = LocalObservabilityEmitter.METRIC_FIRST_FRAME,
            pageId = pageId,
            actionId = LocalObservabilityEmitter.METRIC_FIRST_FRAME,
            params = mapOf(
                "durationMs" to durationMs.toString(),
                "thresholdMs" to FIRST_FRAME_SLOW_THRESHOLD_MS.toString(),
                "anomaly" to anomaly.toString(),
            ),
        )
    }

    fun onScrollFpsSample(
        pageId: String,
        fps: Float,
        frameCount: Int,
        emit: PerformanceEmit,
    ) {
        val anomaly = fps > 0f && fps < SCROLL_FPS_MIN_THRESHOLD
        emit.emit(
            eventOrMetric = LocalObservabilityEmitter.METRIC_SCROLL_FPS,
            pageId = pageId,
            actionId = LocalObservabilityEmitter.METRIC_SCROLL_FPS,
            params = mapOf(
                "fps" to "%.1f".format(fps),
                "frameCount" to frameCount.toString(),
                "fpsThreshold" to SCROLL_FPS_MIN_THRESHOLD.toString(),
                "anomaly" to anomaly.toString(),
            ),
        )
    }

    fun onPageDwell(
        pageId: String,
        dwellMs: Long,
        emit: PerformanceEmit,
    ) {
        emit.emit(
            eventOrMetric = LocalObservabilityEmitter.METRIC_PAGE_DWELL,
            pageId = pageId,
            actionId = LocalObservabilityEmitter.METRIC_PAGE_DWELL,
            params = mapOf(
                "dwellMs" to dwellMs.toString(),
                "anomaly" to "false",
            ),
        )
    }
}
