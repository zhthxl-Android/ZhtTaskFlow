package com.example.zhttaskflow.exception

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.example.zhttaskflow.core.util.isDebugLoggingEnabled
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Release 崩溃监控装配：ANR 检测
 * 前后台感知、堆栈抓取、防重复上报、检测间隔、优雅停止、信息增强
 */
object ReleaseCrashMonitoring {

    // ========== 常量配置 ==========
    /** ANR 判定阈值：与系统前台 ANR 对齐（5秒） */
    private const val ANR_THRESHOLD_MS: Long = 5_000L

    /** 两轮检测之间的休息间隔，避免死循环空转占 CPU */
    private const val DETECT_INTERVAL_MS: Long = 2_000L

    /** ANR 上报冷却时间：一次 ANR 后 30 秒内不再重复上报，避免刷屏 */
    private const val REPORT_COOLDOWN_MS: Long = 30_000L

    /** 堆栈抓取深度限制，避免堆栈过长导致上报体积过大 */
    private const val MAX_STACK_DEPTH: Int = 50

    @Volatile
    private var installed: Boolean = false

    /** 看门狗线程运行标记，用 AtomicBoolean 保证多线程可见性和原子性 */
    private val running = AtomicBoolean(false)

    /** 看门狗线程引用，用于优雅停止 */
    @Volatile
    private var watchdogThread: Thread? = null

    /** 主线程探针执行延迟（毫秒），用于上报信息增强 */
    @Volatile
    private var mainThreadDelayMs: Long = 0L

    /** 上次 ANR 上报时间戳，用于冷却控制 */
    @Volatile
    private var lastReportTimeMs: Long = 0L

    /** 应用前后台状态，由外部生命周期回调更新 */
    @Volatile
    private var isForeground: Boolean = true

    /**
     * 在 Application.onCreate 调用一次
     */
    fun install(application: Application) {
        if (installed || isDebugLoggingEnabled()) {
            return
        }
        installed = true
        // TODO: Bugly.init(application) — 通常已包含 Java 崩溃 + ANR
        registerActivityLifecycleCallbacks(application)
        installMainThreadAnrWatchdog()
    }

    /**
     * 外部可调用：优雅停止 ANR 监控（一般不需要调用，进程结束自动销毁）
     */
    fun stop() {
        running.set(false)
        watchdogThread?.interrupt()
        watchdogThread = null
    }

    /**
     * 外部可调用：手动更新前后台状态（如果不想用 Activity 生命周期监听）
     */
    fun updateForegroundState(foreground: Boolean) {
        isForeground = foreground
    }

    // ========== 核心：ANR 看门狗 ==========
    private fun installMainThreadAnrWatchdog() {
        val mainHandler = Handler(Looper.getMainLooper())

        watchdogThread = Thread(
            {
                running.set(true)
                // 看门狗线程优先级设置为后台低优先级，避免影响主线程
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

                while (running.get() && !Thread.currentThread().isInterrupted) {
                    try {
                        // 后台不检测，避免后台卡顿误报
                        if (!isForeground) {
                            Thread.sleep(DETECT_INTERVAL_MS)
                            continue
                        }

                        // 记录探针投递时间，用于计算实际卡顿时长
                        val postTime = SystemClock.uptimeMillis()
                        val latch = CountDownLatch(1)

                        // 探针任务里记录主线程实际执行时间
                        mainHandler.post {
                            val executeTime = SystemClock.uptimeMillis()
                            // 计算探针消息在主线程消息队列里的排队等待时间
                            mainThreadDelayMs = executeTime - postTime
                            latch.countDown()
                        }

                        // 等待主线程响应
                        val responded = latch.await(
                            ANR_THRESHOLD_MS,
                            TimeUnit.MILLISECONDS
                        )

                        if (!responded) {
                            // 超时后二次确认，避免主线程刚好在执行长任务的边界误判
                            val confirmLatch = CountDownLatch(1)
                            mainHandler.post { confirmLatch.countDown() }
                            val stillBlocked = !confirmLatch.await(
                                1_000L,
                                TimeUnit.MILLISECONDS
                            )

                            if (stillBlocked) {
                                handleAnrDetected(postTime)
                            }
                        }

                        // 每轮检测后休息一段时间，避免死循环空转
                        Thread.sleep(DETECT_INTERVAL_MS)

                    } catch (e: InterruptedException) {
                        // 被中断，退出循环
                        Thread.currentThread().interrupt()
                        break
                    } catch (e: Exception) {
                        // 看门狗自身异常不能崩，吞掉继续
                    }
                }
            },
            "App-AnrWatchdog"
        )

        watchdogThread?.isDaemon = true
        watchdogThread?.start()
    }

    // ========== ANR 处理与上报 ==========
    private fun handleAnrDetected(postTime: Long) {
        // 冷却控制，避免一次卡顿连续上报刷屏
        val now = SystemClock.uptimeMillis()
        if (now - lastReportTimeMs < REPORT_COOLDOWN_MS) {
            return
        }
        lastReportTimeMs = now

        // 抓取主线程堆栈，这是定位 ANR 根因的核心
        val mainThreadStack = dumpMainThreadStack()
        // 抓取所有线程堆栈，方便排查死锁
        val allThreadStack = dumpAllThreadsStack()

        val anrInfo = buildString {
            append("ANR detected\n")
            append("thresholdMs=").append(ANR_THRESHOLD_MS).append('\n')
            append("blockDurationMs≈").append(now - postTime).append('\n')
            append("lastProbeDelayMs=").append(mainThreadDelayMs).append('\n')
            append("isForeground=").append(isForeground).append('\n')
            append("===== Main Thread Stack =====\n")
            append(mainThreadStack)
            append("\n===== All Threads Summary =====\n")
            append(allThreadStack)
        }

        ReleaseCrashReporter.reportAnr(threadDump = anrInfo)
    }

    // ========== 堆栈抓取工具 ==========
    /**
     * 抓取主线程当前调用堆栈
     */
    private fun dumpMainThreadStack(): String {
        val mainLooper = Looper.getMainLooper()
        val mainThread = mainLooper.thread
        val stackTrace = mainThread.stackTrace

        return buildString {
            append(mainThread.name).append(" prio=").append(mainThread.priority).append('\n')
            val depth = minOf(
                stackTrace.size,
                MAX_STACK_DEPTH
            )
            for (i in 0 until depth) {
                append("\tat ").append(stackTrace[i]).append('\n')
            }
            if (stackTrace.size > MAX_STACK_DEPTH) {
                append("\t... ").append(stackTrace.size - MAX_STACK_DEPTH).append(" more\n")
            }
        }
    }

    /**
     * 抓取所有线程的名称和状态摘要（不抓完整堆栈，控制体积）
     */
    private fun dumpAllThreadsStack(): String {
        return buildString {
            val allThreads = Thread.getAllStackTraces()
            append("Total threads: ").append(allThreads.size).append('\n')
            for ((thread, stack) in allThreads) {
                append("- ").append(thread.name)
                append(" state=").append(thread.state)
                    .append(" stackDepth=").append(stack.size)
                    .append('\n')
            }
        }
    }

    // ========== 前后台监听 ==========
    /**
     * 通过 Activity 生命周期自动感知前后台
     */
    private fun registerActivityLifecycleCallbacks(application: Application) {
        var activityCount = 0
        application.registerActivityLifecycleCallbacks(object :
                                                           android.app.Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: android.app.Activity) {
                activityCount++
                isForeground = true
            }

            override fun onActivityPaused(activity: android.app.Activity) {
                activityCount--
                //当所有 Activity 都进入暂停状态
                if (activityCount <= 0) {
                    isForeground = false
                    activityCount = 0
                }
            }

            // 其他回调空实现
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
