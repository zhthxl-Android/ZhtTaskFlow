package com.example.zhttaskflow.base.exception

import com.example.zhttaskflow.base.observability.DeveloperObservability

/**
 * 非 Composable 场景（未捕获异常钩子、[ExceptionHandler.reportCrash]）解析当前 Reporter。
 */
internal object CrashReporterRegistry {

    private val stack = ArrayDeque<CrashReporter>()// 栈

    @Volatile
    private var applicationDefault: CrashReporter? = null// Application 层默认值

    /**
     * [com.example.zhttaskflow.TaskFlowApplication.onCreate] 在 UI 装配前注册默认 Reporter（协程/线程未捕获异常）。
     */
    fun installApplicationDefault(reporter: CrashReporter) {
        applicationDefault = reporter
    }

    fun push(reporter: CrashReporter) {
        stack.addLast(reporter)// 压栈
    }

    fun pop(reporter: CrashReporter) {
        // 只弹出栈顶匹配的（引用相等 ===），防止乱序 pop
        if (stack.isNotEmpty() && stack.last() === reporter) {
            stack.removeLast()
        }
    }

    fun current(): CrashReporter {
        // 优先级：栈顶 > applicationDefault > DebugCrashReporter（兜底）
        val shell = stack.lastOrNull() ?: applicationDefault ?: CrashReporterFallback
        // 再经过 DeveloperObservability 包装一层（运行时可切换）
        return DeveloperObservability.resolveCrashReporter(shell)
    }
}
