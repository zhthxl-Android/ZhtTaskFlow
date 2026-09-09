package com.example.zhttaskflow.base.exception

import com.example.zhttaskflow.base.observability.DeveloperObservability

/**
 * 非 Composable 场景（未捕获异常钩子、[ExceptionHandler.reportCrash]）解析当前 Reporter。
 */
internal object CrashReporterRegistry {

    private val stack = ArrayDeque<CrashReporter>()

    @Volatile
    private var applicationDefault: CrashReporter? = null

    /**
     * [com.example.zhttaskflow.TaskFlowApplication.onCreate] 在 UI 装配前注册默认 Reporter（协程/线程未捕获异常）。
     */
    fun installApplicationDefault(reporter: CrashReporter) {
        applicationDefault = reporter
    }

    fun push(reporter: CrashReporter) {
        stack.addLast(reporter)
    }

    fun pop(reporter: CrashReporter) {
        if (stack.isNotEmpty() && stack.last() === reporter) {
            stack.removeLast()
        }
    }

    fun current(): CrashReporter {
        val shell = stack.lastOrNull() ?: applicationDefault ?: CrashReporterFallback
        return DeveloperObservability.resolveCrashReporter(shell)
    }
}
