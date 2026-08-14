package com.example.zhttaskflow.core.network

import android.content.Context
import android.content.pm.ApplicationInfo

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
