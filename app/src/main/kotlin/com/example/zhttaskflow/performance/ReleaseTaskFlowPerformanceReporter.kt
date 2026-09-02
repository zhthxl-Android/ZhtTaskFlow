package com.example.zhttaskflow.performance

import com.example.zhttaskflow.base.performance.TaskFlowPerformanceReporter
import com.example.zhttaskflow.observability.ReleaseTaskFlowObservabilityContract
import com.example.zhttaskflow.observability.ReleaseTaskFlowObservabilityContract.Channel

/**
 * 生产环境 [TaskFlowPerformanceReporter]：首帧 / 滚动 FPS / 停留时长经统一契约对接 APM。
 */
object ReleaseTaskFlowPerformanceReporter : TaskFlowPerformanceReporter {

    override fun onFirstFrameRendered(pageId: String, durationMs: Long) {
        ReleaseTaskFlowObservabilityContract.emit(
            channel = Channel.PERFORMANCE,
            eventOrMetric = ReleaseTaskFlowObservabilityContract.METRIC_FIRST_FRAME,
            pageId = pageId,
            actionId = ReleaseTaskFlowObservabilityContract.METRIC_FIRST_FRAME,
            params = mapOf(
                "durationMs" to durationMs.toString(),
            ),
        )
    }

    override fun onScrollFpsSample(pageId: String, fps: Float, frameCount: Int) {
        ReleaseTaskFlowObservabilityContract.emit(
            channel = Channel.PERFORMANCE,
            eventOrMetric = ReleaseTaskFlowObservabilityContract.METRIC_SCROLL_FPS,
            pageId = pageId,
            actionId = ReleaseTaskFlowObservabilityContract.METRIC_SCROLL_FPS,
            params = mapOf(
                "fps" to "%.1f".format(fps),
                "frameCount" to frameCount.toString(),
            ),
        )
    }

    override fun onPageDwell(pageId: String, dwellMs: Long) {
        ReleaseTaskFlowObservabilityContract.emit(
            channel = Channel.PERFORMANCE,
            eventOrMetric = ReleaseTaskFlowObservabilityContract.METRIC_PAGE_DWELL,
            pageId = pageId,
            actionId = ReleaseTaskFlowObservabilityContract.METRIC_PAGE_DWELL,
            params = mapOf(
                "dwellMs" to dwellMs.toString(),
            ),
        )
    }
}
