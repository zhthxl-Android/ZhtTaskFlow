package com.example.zhttaskflow.base.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.example.zhttaskflow.core.network.NetworkChecker

/**
 * 全局网络状态检测工具（无状态单例），统一收敛网络判断逻辑。
 *
 * ## 线程安全
 * 纯工具方法、无内部可变状态，可在任意线程调用。
 *
 * ## 与 [com.example.zhttaskflow.core.network.NetworkChecker] 的关系
 * [isNetworkAvailable] 委托 core 层「强无网、宽松有网」规则，避免 VALIDATED 误杀瞬时波动。
 *
 * ## 扩展预留
 * 后续可在此对象中补充蜂窝/以太网类型判断、计费网络（metered）检测等能力。
 */
object NetworkUtil {

    /**
     * 检测当前是否存在可用于联网的网络连接（系统连接态，非外网连通性探测）。
     *
     * @param context 用于获取 [ConnectivityManager]；建议传入 [Context.getApplicationContext] 避免泄漏
     * @return `true` 表示系统存在可用网络连接；`false` 表示明确无网或系统服务不可用
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return NetworkChecker.isNetworkAvailable(context)
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

    private fun isWifiConnectedByCapabilities(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
