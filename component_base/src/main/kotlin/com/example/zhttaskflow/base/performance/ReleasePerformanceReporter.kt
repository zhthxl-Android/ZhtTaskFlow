package com.example.zhttaskflow.base.performance

import com.example.zhttaskflow.base.observability.LocalObservabilityEmitter
import com.example.zhttaskflow.base.observability.ObservabilityEmitPipeline
import com.example.zhttaskflow.base.observability.ReleasePerformanceCore

/**
 * 生产环境 [PerformanceReporter]：指标经统一可观测管道落盘，并由壳层 [com.example.zhttaskflow.base.observability.ObservabilityPlatformHook] 扩展。
 */
object ReleasePerformanceReporter : PerformanceReporter {

    private val emitAdapter = ReleasePerformanceCore.PerformanceEmit { eventOrMetric, pageId, actionId, params ->
        ObservabilityEmitPipeline.emit(
            channel = LocalObservabilityEmitter.Channel.PERFORMANCE,
            eventOrMetric = eventOrMetric,
            pageId = pageId,
            actionId = actionId,
            params = params,
        )
    }

    override fun onFirstFrameRendered(pageId: String, durationMs: Long) {
        ReleasePerformanceCore.onFirstFrameRendered(
            pageId = pageId,
            durationMs = durationMs,
            emit = emitAdapter,
        )
    }

    override fun onScrollFpsSample(pageId: String, fps: Float, frameCount: Int) {
        ReleasePerformanceCore.onScrollFpsSample(
            pageId = pageId,
            fps = fps,
            frameCount = frameCount,
            emit = emitAdapter,
        )
    }

    override fun onPageDwell(pageId: String, dwellMs: Long) {
        ReleasePerformanceCore.onPageDwell(
            pageId = pageId,
            dwellMs = dwellMs,
            emit = emitAdapter,
        )
    }
}
