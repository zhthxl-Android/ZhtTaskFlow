package com.example.zhttaskflow.base.exception

import com.example.zhttaskflow.core.log.Logger
import com.example.zhttaskflow.core.observability.LocalLogStore
import com.example.zhttaskflow.core.util.isDebugLoggingEnabled
import com.example.zhttaskflow.core.util.nullIfBlank
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/** 协程未捕获异常上报 actionId 扩展字段（与 [CRASH_ACTION_ID] 同属崩溃通道）。 */
const val COROUTINE_UNCAUGHT_SOURCE: String = "app_uncaught_coroutine"

/**
 * 全局协程未捕获异常处理器：与 [ExceptionHandler.installUncaughtExceptionHandler] 互补，覆盖协程域。
 *
 * - 在 [install] 时挂载到应用根 [applicationScope]，并注册 [CrashReporterRegistry] 默认实现。
 * - 不处理 [CancellationException]；业务层 `try/catch` 与 [com.example.zhttaskflow.base.mvi.BaseViewModel.launchTask] 逻辑不受影响。
 * - Debug 安装包：Logcat 输出；Release 安装包：仅静默走 [CrashReporter]（与线程未捕获崩溃同一落盘格式）。
 */
object AppCoroutineExceptionHandler {

    //日志统一标签，复用全局崩溃日志标签，保证所有崩溃相关日志可通过同一个 Tag 过滤。
    private const val LOG_TAG: String = CRASH_LOG_TAG

    @Volatile
    private var installedHandler: CoroutineExceptionHandler? = null

    /**
     * 应用级根协程作用域（[install] 之后可用）；基础设施长任务可在此启动。
     */
    @Volatile
    var applicationScope: CoroutineScope? = null
        private set

    /**
     * 已安装的处理器；供其他模块创建 [CoroutineScope] 时并入上下文。
     */
    val coroutineExceptionHandler: CoroutineExceptionHandler
        get() = installedHandler ?: CoroutineExceptionHandler { _, _ -> }

    /**
     * 在 [android.app.Application.onCreate] 调用一次。
     *
     * @param crashReporter 与壳层 [com.example.zhttaskflow.navigation.AppMainShell] 注入策略一致（Debug/Release）。
     */
    fun install(crashReporter: CrashReporter) {
        //存全局默认,把 crashReporter 存到 Registry，UI 还没起来时的兜底。
        CrashReporterRegistry.installApplicationDefault(crashReporter)
        //创建协程处理器
        val handler = createCoroutineExceptionHandler()
        installedHandler = handler
        //挂载到应用根协程作用域
        applicationScope = CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate + handler,
        )
        //安装线程未捕获异常处理器
        ExceptionHandler.installUncaughtExceptionHandler(crashReporter)
    }

    private fun createCoroutineExceptionHandler(): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { context, throwable ->
            handleCoroutineException(context, throwable)
        }
    }

    private fun handleCoroutineException(
        context: CoroutineContext,
        throwable: Throwable,
    ) {
        //忽略协程取消异常
        if (throwable is CancellationException) {
            return
        }
        //获取当前崩溃上报器
        val reporter = CrashReporterRegistry.current()
        //获取异常发生时的页面路径
        val pagePath = LocalLogStore.lastKnownPageId().orEmpty()
        //格式化协程上下文，便于排查
        // 如 "SupervisorJob;HandlerDispatcher;CoroutineName"
        val contextSummary = formatCoroutineContext(context)
        //包装成结构化异常对象
        val reportable = CoroutineUncaughtException(
            pagePath = pagePath,
            coroutineContextSummary = contextSummary,
            cause = throwable,
        )
        //Debug 包输出详细日志
        if (isDebugLoggingEnabled()) {
            Logger.errorAlways(
                LOG_TAG,
                {
                    "[coroutine] pagePath=$pagePath context=$contextSummary " +
                        (throwable.message.nullIfBlank() ?: throwable::class.simpleName.orEmpty())
                },
                throwable,
            )
        }
        //上报异常，标记为非致命
        reporter.reportCrash(reportable, fatal = false)
    }

    /**
     * 格式化协程上下文，便于排查。
     * 将 `CoroutineContext` 中所有元素的类名用分号拼接
     * */
    private fun formatCoroutineContext(context: CoroutineContext): String {
        return buildString {
            //遍历协程上下文
            context.fold(this) { _, element ->
                if (isNotEmpty()) {
                    append(';')
                }
                append(element::class.simpleName.orEmpty())
            }
        }.ifBlank { "unknown" }
    }
}
