package com.example.zhttaskflow.base.exception

import com.example.zhttaskflow.base.analytics.AnalyticsRegistry
import com.example.zhttaskflow.base.analytics.trackUiOutcome
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.ext.SnackbarDispatcher
import com.example.zhttaskflow.base.mvi.BaseViewModel
import com.example.zhttaskflow.core.foundation.userDisplayMessage
import com.example.zhttaskflow.core.log.Logger
import com.example.zhttaskflow.core.util.nullIfBlank
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.cancellation.CancellationException

private const val BUSINESS_ACTION_ID = "app_business_exception"

/**
 * 全局异常与网络离线监控入口（日志、Analytics、Snackbar、顶部横幅）。
 *
 * - **业务异常**（含 [com.example.zhttaskflow.core.foundation.AppException] 及普通 [Exception]）：友好文案走全局 Snackbar，不替代 ViewModel 既有 `onError`。
 * - **崩溃级**（[Error] 及线程未捕获）：仅上报 + 日志，交还系统默认处理器。
 */
object ExceptionHandler {

    private const val LOG_TAG = CRASH_LOG_TAG

    /**
     * 已捕获的业务向异常：展示 Error Snackbar 并写日志（不触发进程退出）。
     */
    fun handleBusinessException(
        throwable: Throwable,
        snackbarDispatcher: SnackbarDispatcher?,
        userMessageFallback: String = BaseViewModel.DEFAULT_USER_MESSAGE_FALLBACK,
    ) {
        // 协程取消，忽略
        if (throwable is CancellationException) {
            return
        }
        // is Error
        if (isCrashThrowable(throwable)) {
            // 静默上报，不弹 Snackbar
            reportCrash(throwable, fatal = false)
            return
        }
        // 是 Exception：走业务异常流程
        val userMessage = throwable.userDisplayMessage(userMessageFallback)
        Logger.errorAlways(LOG_TAG, { "business: $userMessage" }, throwable)
        AnalyticsRegistry.current().trackUiOutcome(
            outcome = "failure",
            operationId = BUSINESS_ACTION_ID,
            pageId = CRASH_PAGE_ID,
            params = mapOf("type" to throwable::class.simpleName.orEmpty()),
        )
        snackbarDispatcher?.showSnackbar(
            message = userMessage,
            type = SnackbarType.Error,
        )
    }

    /**
     * 供根 [kotlinx.coroutines.CoroutineScope] 可选挂载（不修改 [BaseViewModel.launchTask] 既有逻辑）。
     */
    fun coroutineExceptionHandler(
        snackbarDispatcher: SnackbarDispatcher?,
        userMessageFallback: String = BaseViewModel.DEFAULT_USER_MESSAGE_FALLBACK,
        crashReporter: CrashReporter = CrashReporterRegistry.current(),
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            if (isCrashThrowable(throwable)) {
                reportCrash(throwable, fatal = false, crashReporter = crashReporter)
            } else {
                handleBusinessException(
                    throwable = throwable,
                    snackbarDispatcher = snackbarDispatcher,
                    userMessageFallback = userMessageFallback,
                )
            }
        }
    }

    // 崩溃上报：简单委托
    fun reportCrash(
        throwable: Throwable,
        fatal: Boolean,
        crashReporter: CrashReporter = CrashReporterRegistry.current(),
    ) {
        crashReporter.reportCrash(throwable, fatal)
    }

    // 判断是不是崩溃级：只认 Error，不认 Exception
    internal fun isCrashThrowable(throwable: Throwable): Boolean {
        return throwable is Error
    }

    /**
     * 安装 JVM 未捕获异常钩子（进程内仅调用一次）。
     *
     * 须在 [AppCoroutineExceptionHandler.install] 中调用
     * 避免链式 handler 导致同一次崩溃重复落盘。
     *
     * 上报时解析 [CrashReporterRegistry.current]，与协程未捕获异常及壳层 [LocalCrashReporter] 注入一致。
     */
    internal fun installUncaughtExceptionHandler(): () -> Unit {
        //保存当前已有的默认异常处理器
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        //创建自定义的异常处理器
        val handler = Thread.UncaughtExceptionHandler { thread, exception ->
            //自定义崩溃上报：标记为致命异常
            CrashReporterRegistry.current().reportCrash(exception, fatal = true)
            //输出错误日志
            Logger.errorAlways(LOG_TAG, {
                "uncaught thread=${thread.name} ${exception.message.nullIfBlank().orEmpty()}"
            }, exception)
            //自定义处理器处理完后，必须调用前一个处理器（最终是系统默认），否则用户看不到 "应用已停止" 对话框
            previous?.uncaughtException(thread, exception)
        }
        //将自定义处理器设置为 JVM 全局默认
        Thread.setDefaultUncaughtExceptionHandler(handler)
        //返回一个「恢复原处理器」的函数
        return { Thread.setDefaultUncaughtExceptionHandler(previous) }
    }
}
