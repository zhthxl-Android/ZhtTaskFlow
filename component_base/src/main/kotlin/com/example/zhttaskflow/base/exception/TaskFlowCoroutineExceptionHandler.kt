package com.example.zhttaskflow.base.exception

import com.example.zhttaskflow.core.log.TaskFlowLogger
import com.example.zhttaskflow.core.observability.TaskFlowLocalLogStore
import com.example.zhttaskflow.core.util.isTaskFlowDebugLoggingEnabled
import com.example.zhttaskflow.core.util.nullIfBlank
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.cancellation.CancellationException

/** 协程未捕获异常上报 actionId 扩展字段（与 [TASKFLOW_CRASH_ACTION_ID] 同属崩溃通道）。 */
const val TASKFLOW_COROUTINE_UNCAUGHT_SOURCE: String = "app_uncaught_coroutine"

/**
 * 全局协程未捕获异常处理器：与 [TaskFlowExceptionHandler.installUncaughtExceptionHandler] 互补，覆盖协程域。
 *
 * - 在 [install] 时挂载到应用根 [applicationScope]，并注册 [TaskFlowCrashReporterRegistry] 默认实现。
 * - 不处理 [CancellationException]；业务层 `try/catch` 与 [com.example.zhttaskflow.base.mvi.BaseViewModel.launchTask] 逻辑不受影响。
 * - Debug 安装包：Logcat 输出；Release 安装包：仅静默走 [TaskFlowCrashReporter]（与线程未捕获崩溃同一落盘格式）。
 */
object TaskFlowCoroutineExceptionHandler {

    private const val LOG_TAG: String = TASKFLOW_CRASH_LOG_TAG

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
    fun install(crashReporter: TaskFlowCrashReporter) {
        TaskFlowCrashReporterRegistry.installApplicationDefault(crashReporter)
        val handler = createCoroutineExceptionHandler()
        installedHandler = handler
        applicationScope = CoroutineScope(
            SupervisorJob() + Dispatchers.Main.immediate + handler,
        )
        TaskFlowExceptionHandler.installUncaughtExceptionHandler(crashReporter)
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
        if (throwable is CancellationException) {
            return
        }
        val reporter = TaskFlowCrashReporterRegistry.current()
        val pagePath = TaskFlowLocalLogStore.lastKnownPageId().orEmpty()
        val contextSummary = formatCoroutineContext(context)
        val reportable = TaskFlowCoroutineUncaughtException(
            pagePath = pagePath,
            coroutineContextSummary = contextSummary,
            cause = throwable,
        )
        if (isTaskFlowDebugLoggingEnabled()) {
            TaskFlowLogger.errorAlways(
                LOG_TAG,
                {
                    "[coroutine] pagePath=$pagePath context=$contextSummary " +
                        (throwable.message.nullIfBlank() ?: throwable::class.simpleName.orEmpty())
                },
                throwable,
            )
        }
        reporter.reportCrash(reportable, fatal = false)
    }

    private fun formatCoroutineContext(context: CoroutineContext): String {
        return buildString {
            context.fold(this) { _, element ->
                if (isNotEmpty()) {
                    append(';')
                }
                append(element::class.simpleName.orEmpty())
            }
        }.ifBlank { "unknown" }
    }
}

/**
 * 协程未捕获异常包装：堆栈保留原始 cause，消息携带页面与协程上下文摘要。
 */
internal class TaskFlowCoroutineUncaughtException(
    pagePath: String,
    coroutineContextSummary: String,
    cause: Throwable,
) : RuntimeException(
    buildString {
        append(TASKFLOW_COROUTINE_UNCAUGHT_SOURCE)
        append(" pagePath=").append(pagePath)
        append(" context=").append(coroutineContextSummary)
        append(" message=").append(cause.message.nullIfBlank().orEmpty())
    },
    cause,
)
