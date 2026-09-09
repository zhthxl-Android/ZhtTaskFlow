package com.example.zhttaskflow.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.example.zhttaskflow.core.foundation.NetworkException

/**
 * 系统网络连接状态检查（基础设施单例，无业务耦合）。
 *
 * ## 判定策略（强无网、宽松有网）
 * - 仅读取 [ConnectivityManager] + [NetworkCapabilities]，不 ping、不探测外网连通性；
 * - 仅当系统明确无活跃网络或无任何可用传输时返回 `false`；
 * - 存在 Wi-Fi / 蜂窝 / 以太网等连接即返回 `true`，不要求 [NetworkCapabilities.NET_CAPABILITY_VALIDATED]，
 *   避免弱网、假网、校验未完成等瞬时场景误拦截正常请求。
 *
 * ## 线程安全
 * 无内部可变状态，可在任意线程调用。
 */
object NetworkChecker {

    /**
     * 基于宿主已绑定的 Application 上下文判断网络连接态；未绑定时视为有网（与 [safeApiCall] 前置检查一致）。
     */
    fun isNetworkConnected(): Boolean {
        val context = SafeApiCallRuntime.applicationContextOrNull() ?: return true
        return isNetworkAvailable(context)
    }

    /**
     * 强无网场景标准异常（与 [com.example.zhttaskflow.core.network.safeApiCall] 前置失败一致）。
     */
    fun unavailableNetworkException(): NetworkException {
        return NetworkException(
            message = "NetworkChecker: no active network connection",
            userMessage = NetworkUserMessages.NETWORK_IO,
        )
    }

    /**
     * 是否存在可用的网络连接（仅系统层连接态，非外网可达性）。
     *
     * @param context 建议 [Context.getApplicationContext]
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = connectivityManagerOrNull(context) ?: return false
        return isNetworkAvailableByCapabilities(connectivityManager)
    }

    private fun connectivityManagerOrNull(context: Context): ConnectivityManager? {
        val appContext = context.applicationContext
        return ContextCompat.getSystemService(appContext, ConnectivityManager::class.java)
    }

    private fun isNetworkAvailableByCapabilities(connectivityManager: ConnectivityManager): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return hasUsableTransport(capabilities) || capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET,
        )
    }

    private fun hasUsableTransport(capabilities: NetworkCapabilities): Boolean {
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
