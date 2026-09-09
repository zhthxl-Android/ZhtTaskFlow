package com.example.zhttaskflow.base.performance

import com.example.zhttaskflow.core.log.Logger

private const val PERFORMANCE_LOG_TAG = "PagePerformance"

/**
 * 调试默认实现：经 [Logger] 输出，格式稳定便于 Logcat 过滤。
 */
object DebugPerformanceReporter : PerformanceReporter {

    override fun onFirstFrameRendered(pageId: String, durationMs: Long) {
        Logger.d(PERFORMANCE_LOG_TAG) {
            "metric=first_frame pageId=$pageId durationMs=$durationMs"
        }
    }

    override fun onScrollFpsSample(pageId: String, fps: Float, frameCount: Int) {
        Logger.d(PERFORMANCE_LOG_TAG) {
            "metric=scroll_fps pageId=$pageId fps=${"%.1f".format(fps)} frames=$frameCount"
        }
    }

    override fun onPageDwell(pageId: String, dwellMs: Long) {
        Logger.d(PERFORMANCE_LOG_TAG) {
            "metric=dwell pageId=$pageId dwellMs=$dwellMs"
        }
    }
}
