package com.example.zhttaskflow.base.util

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 全局网络状态检测工具（无状态单例），统一收敛网络判断逻辑。
 *
 * ## 线程安全
 * 纯工具方法、无内部可变状态，可在任意线程调用。
 *
 * ## 版本策略
 * - API 23（Android 6.0）及以上：[NetworkCapabilities] 标准能力检测
 * - API 23 以下：[ConnectivityManager.activeNetworkInfo] 兼容路径（项目 minSdk 24，仍保留以符合全版本兼容规范）
 *
 * ## 扩展预留
 * 后续可在此对象中补充蜂窝/以太网类型判断、计费网络（metered）检测等能力。
 */
object NetworkUtil {

    /**
     * 检测当前是否存在可用于联网的有效网络。
     *
     * @param context 用于获取 [ConnectivityManager]；建议传入 [Context.getApplicationContext] 避免泄漏
     * @return `true` 表示至少存在具备 Internet 能力的已连接网络；`false` 表示无网络或系统服务不可用
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = connectivityManagerOrNull(context) ?: return false
        return isNetworkAvailableByCapabilities(connectivityManager)
    }

    /**
     * 检测当前默认网络是否为 Wi-Fi。
     *
     * @param context 用于获取 [ConnectivityManager]
     * @return `true` 表示当前活跃网络为 Wi-Fi；无网络或非 Wi-Fi 时返回 `false`
     */
    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = connectivityManagerOrNull(context) ?: return false
        return isWifiConnectedByCapabilities(connectivityManager)
    }

    private fun connectivityManagerOrNull(context: Context): ConnectivityManager? =
        ContextCompat.getSystemService(context, ConnectivityManager::class.java)

    private fun isNetworkAvailableByCapabilities(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun isWifiConnectedByCapabilities(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
