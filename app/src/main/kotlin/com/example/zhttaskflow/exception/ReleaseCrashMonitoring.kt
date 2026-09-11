package com.example.zhttaskflow.exception

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.example.zhttaskflow.core.util.isDebugLoggingEnabled
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.LockSupport
import kotlin.math.max
import kotlin.math.min

/**
 * Release ANR 监控（标准版 · 最终编译通过版）
 * 核心能力：主线程探针检测、LockSupport零GC、自适应间隔、分层堆栈、前后台分级、阶梯冷却
 * 可靠性保证：并发可见性、虚假唤醒防御、边界兜底、异常保护、编译合规
 */
object ReleaseCrashMonitoring {

    // ==================== 常量配置 ====================
    /** ANR 判定阈值：与系统前台 ANR 对齐（5秒） */
    private const val ANR_THRESHOLD_MS: Long = 5_000L

    /** 二次确认超时：第一次超时后再等1秒确认，避免边界误判 */
    private const val CONFIRM_TIMEOUT_MS: Long = 1_000L

    /** 主线程堆栈最大深度，控制体积和抓取耗时 */
    private const val MAX_MAIN_STACK_DEPTH: Int = 30

    /** 所有线程堆栈最大深度（仅严重ANR时抓取） */
    private const val MAX_ALL_STACK_DEPTH: Int = 15

    // ==================== 自适应间隔配置 ====================
    /** 后台检测间隔：用户无感知，大幅降频 */
    private const val INTERVAL_BACKGROUND_MS: Long = 10_000L

    /** 前台空闲间隔：主线程延迟<500ms */
    private const val INTERVAL_IDLE_MS: Long = 4_000L

    /** 前台正常间隔：主线程延迟500ms~2s */
    private const val INTERVAL_NORMAL_MS: Long = 2_000L

    /** 前台高负载间隔：主线程延迟>2s，加密检测 */
    private const val INTERVAL_BUSY_MS: Long = 1_000L

    // ==================== 阶梯冷却配置 ====================
    /** 重复上报冷却：持续卡顿每2分钟最多报1次 */
    private const val COOLDOWN_REPEAT_MS: Long = 2 * 60 * 1000L

    /** 重复事件窗口：5分钟内的第二次标记为重复 */
    private const val REPEAT_WINDOW_MS: Long = 5 * 60 * 1000L

    // ==================== 状态变量 ====================
    @Volatile
    private var installed: Boolean = false

    /** 看门狗运行开关 */
    private val running = AtomicBoolean(false)

    /** 看门狗线程引用，用于 LockSupport.unpark 和优雅停止 */
    @Volatile
    private var watchdogThread: Thread? = null

    /** 应用前后台状态 */
    @Volatile
    private var isForeground: Boolean = true

    /** 最近一次探针延迟，用于自适应间隔计算 */
    @Volatile
    private var lastProbeDelayMs: Long = 0L

    /** 上次ANR上报时间 */
    @Volatile
    private var lastAnrReportTimeMs: Long = 0L

    /** 线程池初始化标记（替代 isInitialized 编译问题） */
    private val anrExecutorInitialized = AtomicBoolean(false)

    /** 异步上报线程池：堆栈抓取和上报不阻塞看门狗循环 */
    private val anrExecutor by lazy {
        anrExecutorInitialized.set(true)
        ThreadPoolExecutor(
            1,
            1,
            60L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(4),
            { r ->
                Thread(
                    r,
                    "Anr-Report-Worker"
                ).apply { isDaemon = true }
            },
            ThreadPoolExecutor.DiscardOldestPolicy()
        )
    }

    // ==================== 对外接口 ====================
    /**
     * 在 Application.onCreate 调用一次
     */
    fun install(application: Application) {
        if (installed || isDebugLoggingEnabled()) {
            return
        }
        installed = true
        // TODO: 第三方崩溃SDK初始化（如Bugly）通常自带Java崩溃+ANR能力
        registerActivityLifecycleCallbacks(application)
        startWatchdog()
    }

    /**
     * 优雅停止监控（一般不需要调用，进程结束自动销毁）
     */
    fun stop() {
        running.set(false)
        watchdogThread?.let {
            LockSupport.unpark(it)
            it.interrupt()
        }
        watchdogThread = null
        // 通过原子标记判断是否已初始化，避免编译错误
        if (anrExecutorInitialized.get()) {
            runCatching { anrExecutor.shutdownNow() }
        }
    }

    /**
     * 手动更新前后台状态（如需外部控制）
     */
    fun updateForegroundState(foreground: Boolean) {
        isForeground = foreground
    }

    // ==================== 核心：看门狗线程 ====================
    private fun startWatchdog() {
        watchdogThread = Thread(
            {
                running.set(true)
                // 设置系统级后台优先级，比普通线程低，不和业务抢CPU
                android.os.Process.setThreadPriority(
                    android.os.Process.THREAD_PRIORITY_BACKGROUND + 2
                )

                val mainHandler = Handler(Looper.getMainLooper())
                val currentThread = Thread.currentThread()

                while (running.get() && !currentThread.isInterrupted) {
                    try {
                        // ---------- 第一步：场景判定 + 计算间隔 ----------
                        val interval = calculateNextInterval()
                        if (!isForeground) {
                            // 后台：不做ANR判定，只休眠
                            parkNanosSafe(interval * 1_000_000L)
                            continue
                        }

                        // ---------- 第二步：投递轻量化探针 ----------
                        val postTime = SystemClock.uptimeMillis()
                        val probeResponded = AtomicBoolean(false)

                        val posted = mainHandler.post {
                            val execTime = SystemClock.uptimeMillis()
                            lastProbeDelayMs = execTime - postTime
                            probeResponded.set(true)
                            LockSupport.unpark(currentThread)
                        }

                        // 投递失败（Looper已退出）直接终止监控
                        if (!posted) {
                            break
                        }

                        // ---------- 第三步：阻塞等待 + 超时判定 ----------
                        val waitedMs = parkNanosSafe(ANR_THRESHOLD_MS * 1_000_000L)
                        // 超时条件：探针未执行 且 实际等待时间达到阈值
                        val isTimeout = !probeResponded.get() && waitedMs >= ANR_THRESHOLD_MS

                        if (isTimeout) {
                            // ---------- 第四步：二次确认，避免边界误判 ----------
                            val confirmPostTime = SystemClock.uptimeMillis()
                            val confirmResponded = AtomicBoolean(false)
                            val confirmPosted = mainHandler.post {
                                confirmResponded.set(true)
                                LockSupport.unpark(currentThread)
                            }

                            var stillBlocked = false
                            if (confirmPosted) {
                                val confirmWaitedMs = parkNanosSafe(CONFIRM_TIMEOUT_MS * 1_000_000L)
                                stillBlocked =
                                    !confirmResponded.get() && confirmWaitedMs >= CONFIRM_TIMEOUT_MS
                            }

                            if (stillBlocked) {
                                // 确认ANR，强制更新延迟值，触发高负载检测间隔
                                lastProbeDelayMs = ANR_THRESHOLD_MS
                                val blockDuration =
                                    SystemClock.uptimeMillis() - confirmPostTime + ANR_THRESHOLD_MS
                                dispatchAnrReport(blockDuration)
                            }
                        }

                        // ---------- 第五步：本轮结束，按间隔休眠 ----------
                        parkNanosSafe(interval * 1_000_000L)

                    } catch (e: Exception) {
                        // 看门狗自身异常不能崩，吞掉继续下一轮
                    }
                }
            },
            "App-AnrWatchdog"
        )

        watchdogThread?.isDaemon = true
        watchdogThread?.start()
    }

    // ==================== 安全park：防御虚假唤醒，返回实际等待时长 ====================
    /**
     * 安全版 parkNanos，循环等待直到时间耗尽，避免虚假唤醒；返回实际等待毫秒数
     */
    private fun parkNanosSafe(nanos: Long): Long {
        val startMs = SystemClock.uptimeMillis()
        var remainingNanos = max(
            0L,
            nanos
        ) // 边界保护：非负
        while (remainingNanos > 0 && running.get() && !Thread.currentThread().isInterrupted) {
            LockSupport.parkNanos(remainingNanos)
            val elapsedMs = SystemClock.uptimeMillis() - startMs
            // 防止溢出：超过阈值直接截断
            remainingNanos = if (elapsedMs > nanos / 1_000_000L) {
                0L
            } else {
                nanos - elapsedMs * 1_000_000L
            }
        }
        return SystemClock.uptimeMillis() - startMs
    }

    // ==================== 自适应间隔计算 ====================
    /**
     * 根据前后台状态和主线程最近负载，动态计算下一轮检测间隔
     */
    private fun calculateNextInterval(): Long {
        return when {
            !isForeground -> INTERVAL_BACKGROUND_MS
            lastProbeDelayMs < 500L -> INTERVAL_IDLE_MS
            lastProbeDelayMs < 2_000L -> INTERVAL_NORMAL_MS
            else -> INTERVAL_BUSY_MS
        }
    }

    // ==================== ANR 上报分发（阶梯冷却） ====================
    private fun dispatchAnrReport(blockDurationMs: Long) {
        val now = SystemClock.uptimeMillis()
        val timeSinceLastReport = now - lastAnrReportTimeMs

        // 三级冷却策略
        when {
            // 2分钟内：直接跳过，持续卡顿不刷屏
            timeSinceLastReport < COOLDOWN_REPEAT_MS -> return
            // 5分钟内：标记为重复ANR，只抓轻量堆栈
            timeSinceLastReport < REPEAT_WINDOW_MS -> {
                lastAnrReportTimeMs = now
                submitAnrReport(
                    blockDurationMs,
                    isRepeat = true
                )
            }
            // 5分钟以上：视为新事件，完整上报
            else -> {
                lastAnrReportTimeMs = now
                submitAnrReport(
                    blockDurationMs,
                    isRepeat = false
                )
            }
        }
    }

    private fun submitAnrReport(
        blockDurationMs: Long,
        isRepeat: Boolean
    ) {
        // 异步执行堆栈抓取和上报，不阻塞看门狗
        anrExecutor.execute {
            handleAnrDetected(
                blockDurationMs,
                isRepeat
            )
        }
    }

    // ==================== ANR 信息组装与上报（分层堆栈） ====================
    private fun handleAnrDetected(
        blockDurationMs: Long,
        isRepeat: Boolean
    ) {
        // 分层抓取：主线程堆栈必抓（轻量）
        val mainStack = dumpMainThreadStack()

        // 仅首次严重ANR才抓所有线程摘要（重量级），重复ANR跳过
        val allThreadInfo = if (!isRepeat) {
            dumpAllThreadsSummary()
        } else {
            "(repeat ANR, skip all threads dump)"
        }

        val anrInfo = buildString {
            append("===== ANR Detected =====\n")
            append("isRepeat=").append(isRepeat).append('\n')
            append("thresholdMs=").append(ANR_THRESHOLD_MS).append('\n')
            append("blockDurationMs≈").append(blockDurationMs).append('\n')
            append("lastProbeDelayMs=").append(lastProbeDelayMs).append('\n')
            append("isForeground=").append(isForeground).append('\n')
            append("\n===== Main Thread Stack =====\n")
            append(mainStack)
            append("\n===== All Threads Summary =====\n")
            append(allThreadInfo)
        }

        ReleaseCrashReporter.reportAnr(threadDump = anrInfo)
    }

    // ==================== 堆栈抓取工具 ====================
    /**
     * 抓取主线程堆栈（限制深度，控制体积和耗时）
     */
    private fun dumpMainThreadStack(): String {
        val mainThread = Looper.getMainLooper().thread
        val stackTrace = mainThread.stackTrace

        return buildString {
            append(mainThread.name)
                .append(" prio=").append(mainThread.priority)
                .append(" state=").append(mainThread.state)
                .append('\n')
            val depth = min(
                stackTrace.size,
                MAX_MAIN_STACK_DEPTH
            )
            for (i in 0 until depth) {
                append("\tat ").append(stackTrace[i]).append('\n')
            }
            if (stackTrace.size > MAX_MAIN_STACK_DEPTH) {
                append("\t... ").append(stackTrace.size - MAX_MAIN_STACK_DEPTH).append(" more\n")
            }
        }
    }

    /**
     * 抓取所有线程摘要（名称+状态+堆栈深度，不抓完整堆栈，控制开销）
     */
    private fun dumpAllThreadsSummary(): String {
        return buildString {
            val allThreads = Thread.getAllStackTraces()
            append("Total threads: ").append(allThreads.size).append('\n')
            var blockedCount = 0
            for ((thread, stack) in allThreads) {
                if (thread.state == Thread.State.BLOCKED) blockedCount++
                append("- ")
                    .append(thread.name)
                    .append(" state=").append(thread.state)
                    .append(" stackDepth=").append(
                        min(
                            stack.size,
                            MAX_ALL_STACK_DEPTH
                        )
                    )
                    .append('\n')
            }
            append("Blocked threads: ").append(blockedCount).append('\n')
        }
    }

    // ==================== 前后台生命周期监听 ====================
    private fun registerActivityLifecycleCallbacks(application: Application) {
        var activityCount = 0
        application.registerActivityLifecycleCallbacks(object :
                                                           Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: android.app.Activity) {
                activityCount++
                isForeground = true
            }

            override fun onActivityPaused(activity: android.app.Activity) {
                activityCount--
                if (activityCount <= 0) {
                    isForeground = false
                    activityCount = 0
                }
            }

            override fun onActivityCreated(
                activity: android.app.Activity,
                savedInstanceState: android.os.Bundle?
            ) {
            }

            override fun onActivityStarted(activity: android.app.Activity) {}
            override fun onActivityStopped(activity: android.app.Activity) {}
            override fun onActivitySaveInstanceState(
                activity: android.app.Activity,
                outState: android.os.Bundle
            ) {
            }

            override fun onActivityDestroyed(activity: android.app.Activity) {}
        })
    }
}
