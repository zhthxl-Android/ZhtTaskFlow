package com.example.zhttaskflow.base.performance

import android.os.SystemClock
import android.view.Choreographer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.zhttaskflow.core.log.TaskFlowLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PERFORMANCE_LOG_TAG = "PagePerformance"

/** 滚动结束判定：距最后一次滚动事件超过该间隔视为停止（毫秒）。 */
private const val SCROLL_IDLE_THRESHOLD_MS: Long = 150L

/** 单次滚动采样至少累计帧数，不足则不输出 FPS（避免噪声）。 */
private const val SCROLL_FPS_MIN_FRAMES: Int = 4

/**
 * APM 上报抽象：对接监控平台时实现本接口并在壳层注入 [TaskFlowPerformance]。
 */
interface TaskFlowPerformanceReporter {

    /** 自 [TaskFlowPerformance.beginPage] 至首帧绘制完成的耗时。 */
    fun onFirstFrameRendered(pageId: String, durationMs: Long)

    /**
     * 滚动阶段帧率采样（仅在用户滚动期间低开销统计）。
     *
     * @param fps 该段滚动平均帧率
     * @param frameCount 采样帧数
     */
    fun onScrollFpsSample(pageId: String, fps: Float, frameCount: Int)

    /** 页面离开时的停留时长（自 [beginPage] 至 [endPage]）。 */
    fun onPageDwell(pageId: String, dwellMs: Long)
}

/**
 * 调试默认实现：经 [TaskFlowLogger] 输出，格式稳定便于 Logcat 过滤。
 */
object TaskFlowDebugPerformanceReporter : TaskFlowPerformanceReporter {

    override fun onFirstFrameRendered(pageId: String, durationMs: Long) {
        TaskFlowLogger.d(PERFORMANCE_LOG_TAG) {
            "metric=first_frame pageId=$pageId durationMs=$durationMs"
        }
    }

    override fun onScrollFpsSample(pageId: String, fps: Float, frameCount: Int) {
        TaskFlowLogger.d(PERFORMANCE_LOG_TAG) {
            "metric=scroll_fps pageId=$pageId fps=${"%.1f".format(fps)} frames=$frameCount"
        }
    }

    override fun onPageDwell(pageId: String, dwellMs: Long) {
        TaskFlowLogger.d(PERFORMANCE_LOG_TAG) {
            "metric=dwell pageId=$pageId dwellMs=$dwellMs"
        }
    }
}

/**
 * 页面性能会话：首帧、滚动 FPS、停留时长。
 *
 * 由壳层 [LocalTaskFlowPerformance] 提供；业务通过 [PageLifecycleLog][com.example.zhttaskflow.base.ui.extension.PageLifecycleLog]
 * 或手动 [beginPage]/[endPage] 绑定 [pageId]。
 *
 * 产品 APM：实现 [TaskFlowPerformanceReporter]，在应用壳 [com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold]
 * 的 `performanceImpl` 参数注入，与 [com.example.zhttaskflow.base.analytics.TaskFlowAnalytics] 对称。
 */
@Stable
class TaskFlowPerformance internal constructor(
    private val reporter: TaskFlowPerformanceReporter,
) {

    internal companion object {
        val NoOp: TaskFlowPerformance = TaskFlowPerformance(reporter = object : TaskFlowPerformanceReporter {
            override fun onFirstFrameRendered(pageId: String, durationMs: Long) = Unit
            override fun onScrollFpsSample(pageId: String, fps: Float, frameCount: Int) = Unit
            override fun onPageDwell(pageId: String, dwellMs: Long) = Unit
        })
    }

    private val pageIdState = mutableStateOf<String?>(null)

    /** 当前会话页面 ID（Compose 订阅请使用 [pageIdState]）。 */
    val currentPageId: String?
        get() = pageIdState.value

    private var pageEnterUptimeMs: Long = 0L
    private var firstFrameReportedForPage: String? = null

    private val scrollSamplingState = mutableStateOf(false)
    private var scrollIdleJob: Job? = null
    private var scrollSampleFrameCount: Int by mutableIntStateOf(0)
    private var scrollSampleStartNanos: Long by mutableLongStateOf(0L)
    private var scrollSampleEndNanos: Long by mutableLongStateOf(0L)

    internal val activePageIdState = pageIdState

    /** 是否处于滚动采样中（供帧回调挂载）。 */
    val scrolling: Boolean
        get() = scrollSamplingState.value

    internal val scrollSamplingActiveState = scrollSamplingState

    fun beginPage(pageId: String) {
        if (pageId.isBlank()) {
            return
        }
        pageIdState.value = pageId
        pageEnterUptimeMs = SystemClock.uptimeMillis()
        firstFrameReportedForPage = null
        resetScrollSample()
    }

    fun endPage(pageId: String) {
        if (pageIdState.value != pageId) {
            return
        }
        val dwellMs = (SystemClock.uptimeMillis() - pageEnterUptimeMs).coerceAtLeast(0L)
        reporter.onPageDwell(pageId = pageId, dwellMs = dwellMs)
        flushScrollFpsSample(pageId)
        pageIdState.value = null
        pageEnterUptimeMs = 0L
        scrollSamplingState.value = false
        scrollIdleJob?.cancel()
        scrollIdleJob = null
    }

    internal fun enterUptimeMs(): Long = pageEnterUptimeMs

    internal fun markFirstFrameReported(pageId: String) {
        firstFrameReportedForPage = pageId
    }

    internal fun shouldReportFirstFrame(pageId: String): Boolean {
        return firstFrameReportedForPage != pageId
    }

    internal fun reportFirstFrame(pageId: String, durationMs: Long) {
        if (!shouldReportFirstFrame(pageId)) {
            return
        }
        markFirstFrameReported(pageId)
        reporter.onFirstFrameRendered(pageId = pageId, durationMs = durationMs)
    }

    internal fun notifyScrollActivity(scope: CoroutineScope) {
        val pageId = pageIdState.value ?: return
        if (!scrollSamplingState.value) {
            scrollSamplingState.value = true
            scrollSampleFrameCount = 0
            scrollSampleStartNanos = 0L
        }
        scrollIdleJob?.cancel()
        scrollIdleJob = scope.launch {
            delay(SCROLL_IDLE_THRESHOLD_MS)
            scrollSamplingState.value = false
            flushScrollFpsSample(pageId)
        }
    }

    internal fun onScrollFrame(frameTimeNanos: Long) {
        if (!scrollSamplingState.value) {
            return
        }
        if (scrollSampleFrameCount == 0) {
            scrollSampleStartNanos = frameTimeNanos
        }
        scrollSampleFrameCount++
        scrollSampleEndNanos = frameTimeNanos
    }

    private fun flushScrollFpsSample(pageId: String) {
        val frames = scrollSampleFrameCount
        val startNanos = scrollSampleStartNanos
        val endNanos = scrollSampleEndNanos
        resetScrollSample()
        if (frames < SCROLL_FPS_MIN_FRAMES || startNanos <= 0L || endNanos <= startNanos) {
            return
        }
        val durationSec = (endNanos - startNanos) / 1_000_000_000f
        val fps = frames / durationSec
        reporter.onScrollFpsSample(pageId = pageId, fps = fps, frameCount = frames)
    }

    private fun resetScrollSample() {
        scrollSampleFrameCount = 0
        scrollSampleStartNanos = 0L
        scrollSampleEndNanos = 0L
    }

    internal fun nestedScrollConnection(
        scope: CoroutineScope,
    ): NestedScrollConnection {
        return rememberScrollConnection(scope)
    }

    private fun rememberScrollConnection(
        scope: CoroutineScope,
    ): NestedScrollConnection {
        return object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (consumed.y != 0f || available.y != 0f) {
                    notifyScrollActivity(scope)
                }
                return Offset.Zero
            }
        }
    }
}

/**
 * 向下提供 [TaskFlowPerformance]；未注入时返回 [TaskFlowPerformance.NoOp]。
 */
val LocalTaskFlowPerformance = compositionLocalOf { TaskFlowPerformance.NoOp }

@Composable
fun rememberTaskFlowPerformance(): TaskFlowPerformance {
    return LocalTaskFlowPerformance.current
}

/**
 * 由壳层注入的 [reporter] 构造 [TaskFlowPerformance]（默认 [TaskFlowDebugPerformanceReporter]）。
 */
@Composable
fun rememberTaskFlowDebugPerformance(
    reporter: TaskFlowPerformanceReporter = TaskFlowDebugPerformanceReporter,
): TaskFlowPerformance {
    return remember(reporter) { TaskFlowPerformance(reporter) }
}

/**
 * 非 Composable 场景根据 [TaskFlowPerformanceReporter] 创建实例（与 [rememberTaskFlowDebugPerformance] 一致）。
 */
fun createTaskFlowPerformance(
    reporter: TaskFlowPerformanceReporter = TaskFlowDebugPerformanceReporter,
): TaskFlowPerformance = TaskFlowPerformance(reporter)

/**
 * 壳层装配性能监控（与 [com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold] 配合）。
 */
@Composable
fun TaskFlowPerformanceCompositionRoot(
    performance: TaskFlowPerformance = rememberTaskFlowDebugPerformance(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalTaskFlowPerformance provides performance) {
        content()
    }
}

/**
 * Scaffold 内容区：挂载滚动 nestedScroll 与首帧 / 滚动 FPS 副作用（低开销，仅在有 [currentPageId] 时工作）。
 */
@Composable
internal fun TaskFlowPerformanceScaffoldBindings(
    performance: TaskFlowPerformance,
    contentModifier: Modifier,
): Modifier {
    val scope = rememberCoroutineScope()
    val scrollConnection = remember(performance, scope) {
        performance.nestedScrollConnection(scope)
    }
    val pageId by performance.activePageIdState

    TaskFlowPerformanceFirstFrameEffect(performance = performance, pageId = pageId)
    TaskFlowPerformanceScrollFpsEffect(performance = performance)

  return contentModifier.nestedScroll(scrollConnection)
}

@Composable
private fun TaskFlowPerformanceFirstFrameEffect(
    performance: TaskFlowPerformance,
    pageId: String?,
) {
    LaunchedEffect(pageId) {
        val id = pageId ?: return@LaunchedEffect
        if (!performance.shouldReportFirstFrame(id)) {
            return@LaunchedEffect
        }
        val enterMs = performance.enterUptimeMs()
        if (enterMs <= 0L) {
            return@LaunchedEffect
        }
        withFrameMillis { }
        val durationMs = (SystemClock.uptimeMillis() - enterMs).coerceAtLeast(0L)
        performance.reportFirstFrame(pageId = id, durationMs = durationMs)
    }
}

@Composable
private fun TaskFlowPerformanceScrollFpsEffect(
    performance: TaskFlowPerformance,
) {
    val scrolling by performance.scrollSamplingActiveState
    DisposableEffect(scrolling, performance) {
        if (!scrolling) {
            return@DisposableEffect onDispose { }
        }
        val choreographer = Choreographer.getInstance()
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                performance.onScrollFrame(frameTimeNanos)
                if (performance.scrolling) {
                    choreographer.postFrameCallback(this)
                }
            }
        }
        choreographer.postFrameCallback(callback)
        onDispose {
            choreographer.removeFrameCallback(callback)
        }
    }
}
