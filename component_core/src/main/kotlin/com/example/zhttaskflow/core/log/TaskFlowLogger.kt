package com.example.zhttaskflow.core.log

import android.util.Log
import com.example.zhttaskflow.core.network.isTaskFlowDebugLoggingEnabled

/**
 * 全项目统一日志门面（唯一推荐实现）。
 *
 * - 全局 Tag 前缀 `TaskFlow/` 与历史 base 层 Logger 一致；
 * - [isTaskFlowDebugLoggingEnabled] 集中管控 Debug/Release 策略：Debug 安装包输出，Release 跳过；
 * - 消息统一 lambda 懒加载，Release 下无字符串拼接开销；
 * - 带 [Throwable] 的重载与 `android.util.Log` 行为等价；数据层诊断堆栈另提供 [d] 拼接格式重载。
 */
object TaskFlowLogger {

    private const val GLOBAL_TAG_PREFIX = "TaskFlow"

    private fun resolveTag(tag: String): String = "$GLOBAL_TAG_PREFIX/$tag"

    private inline fun logWhenDebugEnabled(block: () -> Unit) {
        if (!isTaskFlowDebugLoggingEnabled()) {
            return
        }
        block()
    }

    fun d(tag: String, message: () -> String) {
        logWhenDebugEnabled {
            Log.d(resolveTag(tag), message())
        }
    }

    /**
     * 数据层诊断：摘要 + 完整堆栈文本（级别 Debug，格式与历史 `safeApiCall` / Room 日志一致）。
     */
    fun d(tag: String, throwable: Throwable, message: () -> String) {
        logWhenDebugEnabled {
            Log.d(
                resolveTag(tag),
                "${message()}\n${Log.getStackTraceString(throwable)}",
            )
        }
    }

    fun i(tag: String, message: () -> String) {
        logWhenDebugEnabled {
            Log.i(resolveTag(tag), message())
        }
    }

    fun w(tag: String, message: () -> String, throwable: Throwable? = null) {
        logWhenDebugEnabled {
            val resolvedTag = resolveTag(tag)
            val text = message()
            if (throwable != null) {
                Log.w(resolvedTag, text, throwable)
            } else {
                Log.w(resolvedTag, text)
            }
        }
    }

    fun e(tag: String, message: () -> String, throwable: Throwable? = null) {
        logWhenDebugEnabled {
            val resolvedTag = resolveTag(tag)
            val text = message()
            if (throwable != null) {
                Log.e(resolvedTag, text, throwable)
            } else {
                Log.e(resolvedTag, text)
            }
        }
    }

    /**
     * 表现层协程失败等必选错误出口：始终 Error 级落盘，不受 Debug 诊断开关影响。
     */
    fun errorAlways(tag: String, message: () -> String, throwable: Throwable? = null) {
        val resolvedTag = resolveTag(tag)
        val text = message()
        if (throwable != null) {
            Log.e(resolvedTag, text, throwable)
        } else {
            Log.e(resolvedTag, text)
        }
    }
}
