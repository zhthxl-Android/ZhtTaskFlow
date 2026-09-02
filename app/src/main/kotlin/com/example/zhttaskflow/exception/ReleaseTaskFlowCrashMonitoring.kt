package com.example.zhttaskflow.exception

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.example.zhttaskflow.core.util.isTaskFlowDebugLoggingEnabled
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Release 崩溃监控装配：预留 ANR 检测与第三方 SDK 初始化入口（Debug 包不启用）。
 */
object ReleaseTaskFlowCrashMonitoring {

    private const val ANR_THRESHOLD_MS: Long = 5_000L

    @Volatile
    private var installed: Boolean = false

    /**
     * 在 [Application.onCreate] 调用一次；无 SDK 时使用主线程响应探测并回调 [ReleaseTaskFlowCrashReporter.reportAnr]。
     */
    fun install(application: Application) {
        if (installed || isTaskFlowDebugLoggingEnabled()) {
            return
        }
        installed = true
        // TODO: Bugly.init(application) — 通常已包含 Java 崩溃 + ANR
        installMainThreadAnrWatchdog()
    }

    private fun installMainThreadAnrWatchdog() {
        val mainHandler = Handler(Looper.getMainLooper())
        val watchdogThread = Thread(
            {
                while (!Thread.currentThread().isInterrupted) {
                    val latch = CountDownLatch(1)
                    mainHandler.post { latch.countDown() }
                    val responded = latch.await(ANR_THRESHOLD_MS, TimeUnit.MILLISECONDS)
                    if (!responded) {
                        ReleaseTaskFlowCrashReporter.reportAnr(
                            threadDump = "mainLooperNotResponding thresholdMs=$ANR_THRESHOLD_MS",
                        )
                    }
                }
            },
            "TaskFlow-AnrWatchdog",
        )
        watchdogThread.isDaemon = true
        watchdogThread.start()
    }
}
