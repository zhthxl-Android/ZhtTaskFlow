package com.example.zhttaskflow.core.util

import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.zhttaskflow.core.network.SafeApiCallRuntime

/**
 * 运行时诊断开关：由宿主 [bindNetworkDiagnostics] 或 [com.example.zhttaskflow.core.network.RetrofitServiceFactory.createApi] 兜底同步。
 */
object RuntimeUtils {

    @Volatile
    var isDebuggable: Boolean = false

    @Volatile
    private var initialized: Boolean = false

    fun syncFrom(context: Context) {
        isDebuggable = context.isAppDebuggable()
        SafeApiCallRuntime.bindContext(context)
        initialized = true
    }

    /**
     * 未初始化时从 [context] 同步一次；已初始化则直接返回（幂等）。
     */
    fun ensureSyncFrom(context: Context) {
        if (initialized) {
            return
        }
        syncFrom(context)
    }
}

/**
 * 通过宿主 [ApplicationInfo.FLAG_DEBUGGABLE] 判断是否为可调试构建。
 *
 * 不依赖各模块 [android.os.Build] 或模块级 BuildConfig，基础库随宿主 App 的 Debug/Release 变体自动生效。
 *
 * @return 宿主应用为 debuggable 时返回 true（通常对应 Debug 安装包）
 */
internal fun Context.isAppDebuggable(): Boolean {
    val applicationInfo = applicationContext.applicationInfo
    return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

/**
 * 宿主 Application 正式初始化入口：启动时同步 Debug/Release 下数据层日志策略。
 */
fun bindNetworkDiagnostics(context: Context) {
    RuntimeUtils.syncFrom(context)
}

/** 是否输出数据层 Debug 诊断日志（与 Debug 安装包一致）。 */
fun isDebugLoggingEnabled(): Boolean = RuntimeUtils.isDebuggable
