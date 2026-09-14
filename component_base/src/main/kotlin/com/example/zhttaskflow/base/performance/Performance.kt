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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 滚动结束判定：距最后一次滚动事件超过该间隔视为停止（毫秒）。 */
private const val SCROLL_IDLE_THRESHOLD_MS: Long = 150L

/** 单次滚动采样至少累计帧数，不足则不输出 FPS（避免噪声）。 */
private const val SCROLL_FPS_MIN_FRAMES: Int = 4

/**
 * APM 上报抽象：对接监控平台时实现本接口并在壳层注入 [Performance]。
 *
 * Release 默认实现见 `app` 模块 `ReleasePerformanceReporter`；指标上报字段与埋点共用 `pageId`，
 * 指标名作为契约中的 `actionId` / `event`（见 `docs/ARCHITECTURE.md` §8.2）。
 */
interface PerformanceReporter {

    /**
     * 页面首帧渲染完成时回调，上报首帧耗时
     * 自 [Performance.beginPage] 至首帧绘制完成的耗时。
     * @param pageId 页面 ID
     * @param durationMs 首帧耗时（毫秒）
     * */
    fun onFirstFrameRendered(
        pageId: String,
        durationMs: Long
    )

    /**
     * 滚动阶段帧率采样（仅在用户滚动期间低开销统计）。
     *
     * @param fps 本次滚动的平均帧率（帧总数 / 采样总时长）
     * @param frameCount 本次采样累计的帧数，用于校验数据可信度
     */
    fun onScrollFpsSample(
        pageId: String,
        fps: Float,
        frameCount: Int
    )

    /** 页面离开时的停留时长（自 [beginPage] 至 [endPage]）。 */
    fun onPageDwell(
        pageId: String,
        dwellMs: Long
    )
}

/**
 * 核心性能状态管理类
 * 页面性能会话：首帧、滚动 FPS、停留时长。
 *
 * 由壳层 [LocalPerformance] 提供；业务通过 [PageLifecycleLog][com.example.zhttaskflow.base.ui.extension.PageLifecycleLog]
 * 或手动 [beginPage]/[endPage] 绑定 [pageId]。
 *
 * 产品 APM：实现 [PerformanceReporter]，在应用壳 [com.example.zhttaskflow.navigation.AppMainShell] 传入
 * `performanceImpl`，或经 [com.example.zhttaskflow.base.ui.BaseScaffold] 的 `performanceImpl` 参数注入，
 * 与 [com.example.zhttaskflow.base.analytics.Analytics] 对称。
 */
@Stable
class Performance internal constructor(
    private val reporter: PerformanceReporter,
) {

    internal companion object {
        //默认使用空实现，保证所有调用都不会空崩溃，同时功能降级为不采集不上报
        val NoOp: Performance = Performance(reporter = object : PerformanceReporter {
            override fun onFirstFrameRendered(
                pageId: String,
                durationMs: Long
            ) = Unit

            override fun onScrollFpsSample(
                pageId: String,
                fps: Float,
                frameCount: Int
            ) = Unit

            override fun onPageDwell(
                pageId: String,
                dwellMs: Long
            ) = Unit
        })
    }

    //当前活跃页面的 ID，Compose 可观察状态，变化会触发重组
    private val pageIdState = mutableStateOf<String?>(null)

    /** 当前会话页面 ID（Compose 订阅请使用 [pageIdState]）。 */
    val currentPageId: String?
        get() = pageIdState.value

    //页面进入的时间戳
    private var pageEnterUptimeMs: Long = 0L

    //记录已经上报过首帧的页面 ID，保证每个页面首帧只上报一次，防止重复上报
    private var firstFrameReportedForPage: String? = null

    //是否正在滚动采样中，Compose 可观察状态，用于驱动 Choreographer 回调的注册与移除
    private val scrollSamplingState = mutableStateOf(false)

    //滚动空闲延时任务，滚动事件触发后延时 150ms 执行，若期间有新滚动则取消重计时
    private var scrollIdleJob: Job? = null

    //本次滚动采样累计的帧数
    private var scrollSampleFrameCount: Int by mutableIntStateOf(0)

    //本次滚动采样第一帧的时间戳，单位纳秒
    private var scrollSampleStartNanos: Long by mutableLongStateOf(0L)

    //本次滚动采样最后一帧的时间戳，单位纳秒
    private var scrollSampleEndNanos: Long by mutableLongStateOf(0L)

    internal val activePageIdState = pageIdState

    /** 是否处于滚动采样中（供帧回调挂载）。 */
    val scrolling: Boolean
        get() = scrollSamplingState.value

    internal val scrollSamplingActiveState = scrollSamplingState

    /**
     * 标记页面开始，启动性能统计
     * @param pageId 页面 ID
     * */
    fun beginPage(pageId: String) {
        if (pageId.isBlank()) {
            return
        }
        //更新当前页面 ID 状态
        pageIdState.value = pageId
        //记录页面进入时间戳
        pageEnterUptimeMs = SystemClock.uptimeMillis()
        //重置首帧上报标记
        firstFrameReportedForPage = null
        //重置滚动采样状态
        resetScrollSample()
    }

    /**
     * 标记页面结束，结算停留时长并上报，收尾所有采样
     * @param pageId 页面 ID
     * */
    fun endPage(pageId: String) {
        //校验和当前页面 ID 一致，避免错误页面触发结束
        if (pageIdState.value != pageId) {
            return
        }
        //计算页面停留时长 = 当前时间 - 进入时间，保底为 0
        val dwellMs = (SystemClock.uptimeMillis() - pageEnterUptimeMs).coerceAtLeast(0L)
        //调用上报器上报停留时长
        reporter.onPageDwell(
            pageId = pageId,
            dwellMs = dwellMs
        )
        //强制上报当前未结算的滚动帧率采样
        flushScrollFpsSample(pageId)
        //清空页面 ID、时间戳，停止滚动采样，取消延时任务
        pageIdState.value = null
        pageEnterUptimeMs = 0L
        scrollSamplingState.value = false
        scrollIdleJob?.cancel()
        scrollIdleJob = null
    }

    internal fun enterUptimeMs(): Long = pageEnterUptimeMs

    /**
     * 标记该页面首帧已上报
     * */
    internal fun markFirstFrameReported(pageId: String) {
        firstFrameReportedForPage = pageId
    }

    /**
     * 判断该页面是否还没上报过首帧
     * */
    internal fun shouldReportFirstFrame(pageId: String): Boolean {
        return firstFrameReportedForPage != pageId
    }

    /**
     * 校验通过后调用上报器上报首帧耗时
     * */
    internal fun reportFirstFrame(
        pageId: String,
        durationMs: Long
    ) {
        if (!shouldReportFirstFrame(pageId)) {
            return
        }
        markFirstFrameReported(pageId)
        reporter.onFirstFrameRendered(
            pageId = pageId,
            durationMs = durationMs
        )
    }

    /**
     * 通知滚动事件发生，启动 / 刷新滚动采样
     * 防抖机制，连续滚动时只持续采样，停止滚动 150ms 后才结算上报
     * @param scope 生命周期作用域，用于挂载滚动空闲延时任务
     * */
    internal fun notifyScrollActivity(scope: CoroutineScope) {
        //无页面 ID 时直接返回
        val pageId = pageIdState.value ?: return
        //首次滚动时开启采样状态
        if (!scrollSamplingState.value) {
            scrollSamplingState.value = true
            scrollSampleFrameCount = 0
            scrollSampleStartNanos = 0L
        }
        //取消上一个空闲延时任务
        scrollIdleJob?.cancel()
        //滚动停止后延迟 150ms 结算并上报一次
        //用来避免滚动过程中频繁上报、也过滤掉手指短暂停顿的误判
        scrollIdleJob = scope.launch {
            delay(SCROLL_IDLE_THRESHOLD_MS)
            scrollSamplingState.value = false
            flushScrollFpsSample(pageId)
        }
    }

    /**
     * 每帧回调，累计滚动采样的帧数和时间
     * @param frameTimeNanos 当前帧的时间戳，单位纳秒，由 Choreographer 提供
     * */
    internal fun onScrollFrame(frameTimeNanos: Long) {
        //非采样状态直接返回
        if (!scrollSamplingState.value) {
            return
        }
        //第一帧时记录采样起始时间
        if (scrollSampleFrameCount == 0) {
            scrollSampleStartNanos = frameTimeNanos
        }
        //帧数 + 1，更新采样结束时间为当前帧时间
        scrollSampleFrameCount++
        scrollSampleEndNanos = frameTimeNanos
    }

    /**
     * 结算本次滚动采样，计算平均 FPS 并上报
     * */
    private fun flushScrollFpsSample(pageId: String) {
        //取出本次采样的帧数、起止时间
        val frames = scrollSampleFrameCount
        val startNanos = scrollSampleStartNanos
        val endNanos = scrollSampleEndNanos
        //重置采样状态
        resetScrollSample()
        //校验：帧数不足最小阈值、时间非法则丢弃
        if (frames < SCROLL_FPS_MIN_FRAMES || startNanos <= 0L || endNanos <= startNanos) {
            return
        }
        //计算采样总时长（秒）= 纳秒差 / 1_000_000_000f
        val durationSec = (endNanos - startNanos) / 1_000_000_000f
        //计算平均帧率 = 总帧数 / 总时长（秒）
        val fps = frames / durationSec
        //调用上报器上报帧率数据
        reporter.onScrollFpsSample(
            pageId = pageId,
            fps = fps,
            frameCount = frames
        )
    }

    /**
     * 重置采样状态
     * */
    private fun resetScrollSample() {
        scrollSampleFrameCount = 0
        scrollSampleStartNanos = 0L
        scrollSampleEndNanos = 0L
    }

    /**
     *创建嵌套滚动连接
     * */
    internal fun nestedScrollConnection(
        scope: CoroutineScope,
    ): NestedScrollConnection {
        return rememberScrollConnection(scope)
    }

    /**
     * 通过 Compose 的NestedScroll机制监听滚动事件
     * */
    private fun rememberScrollConnection(
        scope: CoroutineScope,
    ): NestedScrollConnection {
        return object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                //每次滚动发生时（纵向滚动偏移不为 0)
                if (consumed.y != 0f || available.y != 0f) {
                    //调用`notifyScrollActivity`通知采样
                    notifyScrollActivity(scope)
                }
                return Offset.Zero
            }
        }
    }
}

/**
 * 向下提供 [Performance]；未注入时返回 [Performance.NoOp]。
 */
val LocalPerformance = compositionLocalOf { Performance.NoOp }


/**
 * 获取当前 CompositionLocal 中的 Performance 实例
 * 默认 Performance.NoOp
 * */
@Composable
fun rememberPerformance(): Performance {
    return LocalPerformance.current
}

/**
 * Compose 场景下创建 Performance 实例，默认使用调试版上报器（Debug 环境下打印日志等）
 * 由壳层注入的 [reporter] 构造 [Performance]（默认 [DebugPerformanceReporter]）。
 */
@Composable
fun rememberDebugPerformance(
    reporter: PerformanceReporter = DebugPerformanceReporter,
): Performance {
    return remember(reporter) { Performance(reporter) }
}

/**
 * 非 Compose 场景（如 ViewModel、普通类）创建 Performance 实例
 * 非 Composable 场景根据 [PerformanceReporter] 创建实例（与 [rememberDebugPerformance] 一致）。
 */
fun createPerformance(
    reporter: PerformanceReporter = DebugPerformanceReporter,
): Performance = Performance(reporter)

/**
 * 性能监控的根注入器，放在应用壳层（如 BaseScaffold、Application 级主题下），
 * 将 Performance 实例注入到整个 Compose 树
 * Release 环境传入真实上报实现，Debug 环境传入调试实现
 * 壳层装配性能监控（与 [com.example.zhttaskflow.base.ui.BaseScaffold] 配合）。
 */
@Composable
fun PerformanceCompositionRoot(
    performance: Performance = rememberDebugPerformance(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalPerformance provides performance) {
        content()
    }
}

/**
 * 给页面内容 Modifier 挂载滚动监听
 * Scaffold 内容区：挂载滚动 nestedScroll 与首帧 / 滚动 FPS 副作用（低开销，仅在有 [currentPageId] 时工作）。
 */
@Composable
internal fun PerformanceScaffoldBindings(
    performance: Performance,
    contentModifier: Modifier,
): Modifier {
    val scope = rememberCoroutineScope()
    //创建滚动连接实例
    val scrollConnection = remember(
        performance,
        scope
    ) {
        performance.nestedScrollConnection(scope)
    }
    val pageId by performance.activePageIdState
    //启动首帧采集副作用
    PerformanceFirstFrameEffect(
        performance = performance,
        pageId = pageId
    )
    //启动滚动帧率采集副作用
    PerformanceScrollFpsEffect(performance = performance)
    //给原 Modifier 加上`nestedScroll`修饰
    return contentModifier.nestedScroll(scrollConnection)
}

/**
 * 首帧渲染耗时采集的副作用实现
 * `LaunchedEffect(pageId)` 页面 ID 变化时启动，先等待下一帧绘制完成，再计算耗时
 * */
@Composable
private fun PerformanceFirstFrameEffect(
    performance: Performance,
    pageId: String?,
) {
    LaunchedEffect(pageId) {
        //pageId 为空或已上报过首帧则跳过
        val id = pageId ?: return@LaunchedEffect
        if (!performance.shouldReportFirstFrame(id)) {
            return@LaunchedEffect
        }
        //获取页面进入时间戳
        val enterMs = performance.enterUptimeMs()
        if (enterMs <= 0L) {
            return@LaunchedEffect
        }
        //挂起直到下一帧绘制完成，这是 Compose 提供的等待帧绘制的原语
        withFrameMillis { }
        //计算首帧耗时 = 当前时间 - 页面进入时间
        val durationMs = (SystemClock.uptimeMillis() - enterMs).coerceAtLeast(0L)
        //上报首帧耗时
        performance.reportFirstFrame(
            pageId = id,
            durationMs = durationMs
        )
    }
}

/**
 * 滚动帧率采集的副作用，通过系统 Choreographer 监听真实帧回调
 * `DisposableEffect` 配合 `Choreographer.FrameCallback`，
 * 滚动开始时注册帧回调，滚动结束时移除，避免不必要的开销
 * */
@Composable
private fun PerformanceScrollFpsEffect(
    performance: Performance,
) {
    val scrolling by performance.scrollSamplingActiveState
    DisposableEffect(
        scrolling,
        performance
    ) {
        if (!scrolling) {
            return@DisposableEffect onDispose { }
        }
        //获取主线程 Choreographer 实例
        val choreographer = Choreographer.getInstance()
        //创建帧回调，每帧调用`onScrollFrame`累计帧数
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                performance.onScrollFrame(frameTimeNanos)
                if (performance.scrolling) {
                    //postFrameCallback 注册的回调只触发一次
                    //只要还在滚动就持续监听下一帧
                    choreographer.postFrameCallback(this)
                }
            }
        }
        //注册帧回调
        choreographer.postFrameCallback(callback)
        //滚动结束 / 组件销毁时：移除帧回调，停止采样
        onDispose {
            choreographer.removeFrameCallback(callback)
        }
    }
}
