package com.example.zhttaskflow.performance

import com.example.zhttaskflow.base.performance.PerformanceReporter
import com.example.zhttaskflow.observability.ReleaseObservabilityContract
import com.example.zhttaskflow.observability.ReleaseObservabilityContract.Channel

/**
 * 生产环境 [PerformanceReporter]：指标写入本地 [com.example.zhttaskflow.observability.LocalLogStore]，
 * 慢首帧 / 低帧率自动标记 `anomaly=true`。
 */
object ReleasePerformanceReporter : PerformanceReporter {

    private const val FIRST_FRAME_SLOW_THRESHOLD_MS: Long = 700L
    private const val SCROLL_FPS_MIN_THRESHOLD: Float = 45f

    override fun onFirstFrameRendered(pageId: String, durationMs: Long) {
        val anomaly = durationMs > FIRST_FRAME_SLOW_THRESHOLD_MS
        ReleaseObservabilityContract.emit(
            channel = Channel.PERFORMANCE,
            eventOrMetric = ReleaseObservabilityContract.METRIC_FIRST_FRAME,
            pageId = pageId,
            actionId = ReleaseObservabilityContract.METRIC_FIRST_FRAME,
            params = mapOf(
                "durationMs" to durationMs.toString(),
                "thresholdMs" to FIRST_FRAME_SLOW_THRESHOLD_MS.toString(),
                "anomaly" to anomaly.toString(),
            ),
        )
    }

    override fun onScrollFpsSample(pageId: String, fps: Float, frameCount: Int) {
        val anomaly = fps > 0f && fps < SCROLL_FPS_MIN_THRESHOLD
        ReleaseObservabilityContract.emit(
            channel = Channel.PERFORMANCE,
            eventOrMetric = ReleaseObservabilityContract.METRIC_SCROLL_FPS,
            pageId = pageId,
            actionId = ReleaseObservabilityContract.METRIC_SCROLL_FPS,
            params = mapOf(
                "fps" to "%.1f".format(fps),
                "frameCount" to frameCount.toString(),
                "fpsThreshold" to SCROLL_FPS_MIN_THRESHOLD.toString(),
                "anomaly" to anomaly.toString(),
            ),
        )
    }

    override fun onPageDwell(pageId: String, dwellMs: Long) {
        ReleaseObservabilityContract.emit(
            channel = Channel.PERFORMANCE,
            eventOrMetric = ReleaseObservabilityContract.METRIC_PAGE_DWELL,
            pageId = pageId,
            actionId = ReleaseObservabilityContract.METRIC_PAGE_DWELL,
            params = mapOf(
                "dwellMs" to dwellMs.toString(),
                "anomaly" to "false",
            ),
        )
    }
}
