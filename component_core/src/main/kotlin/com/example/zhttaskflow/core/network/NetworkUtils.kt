package com.example.zhttaskflow.core.network

import android.content.Context
import android.content.pm.ApplicationInfo

/**
 * 网络诊断开关：由 [RetrofitServiceFactory] 在首次构建 OkHttp 时同步宿主 debuggable 状态。
 *
 * 用于 [safeApiCall] 等数据层仅在 Debug 安装包输出详细诊断，Release 不重复打印 Error 堆栈。
 */
internal object TaskFlowNetworkDiagnostics {

    @Volatile
    var isDebuggable: Boolean = false

    fun syncFrom(context: Context) {
        isDebuggable = context.isAppDebuggable()
    }
}

/**
 * 在 Application 或首次发起网络请求前调用，同步 Debug/Release 下 [safeApiCall] 的日志策略。
 */
fun bindTaskFlowNetworkDiagnostics(context: Context) {
    TaskFlowNetworkDiagnostics.syncFrom(context)
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
