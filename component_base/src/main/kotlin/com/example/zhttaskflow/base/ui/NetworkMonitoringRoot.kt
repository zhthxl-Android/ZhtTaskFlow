package com.example.zhttaskflow.base.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import com.example.zhttaskflow.base.R
import com.example.zhttaskflow.core.debug.DeveloperTools
import com.example.zhttaskflow.core.network.NetworkChecker
import com.example.zhttaskflow.core.util.isDebugLoggingEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch


/**
 * 壳层装配：监听网络并在断开时展示顶部横幅。
 *
 * 由 [com.example.zhttaskflow.base.ui.BaseScaffold] 在拥有全局宿主时调用。
 */
@Composable
fun NetworkMonitoringRoot(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var offlineBannerVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current.applicationContext
    NetworkConnectivityMonitor(
        context = context,
        onDisconnected = { offlineBannerVisible = true },
        onConnected = { offlineBannerVisible = false },
    )
    Box(modifier = modifier.fillMaxSize()) {
        content()
        if (offlineBannerVisible) {
            NetworkOfflineBanner(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
            )
        }
    }
}

/**
 * 纯副作用 Composable，不输出 UI，只负责监听网络状态并触发回调
 * */
@Composable
private fun NetworkConnectivityMonitor(
    context: Context,
    onDisconnected: () -> Unit,
    onConnected: () -> Unit,
) {
    val disconnectedState = rememberUpdatedState(onDisconnected)
    val connectedState = rememberUpdatedState(onConnected)
    val scope = rememberCoroutineScope()
    DisposableEffect(context) {
        val connectivityManager = ContextCompat.getSystemService(
            context,
            ConnectivityManager::class.java
        )
            ?: return@DisposableEffect onDispose { }

        fun applyBannerFromNetworkState() {
            //如果处于模拟状态，直接触发回调
            if (DeveloperTools.shouldForceOfflineBanner()) {
                disconnectedState.value()
                return
            }
            //根据网络状态触发回调
            if (NetworkChecker.isNetworkAvailable(context)) {
                connectedState.value()
            } else {
                disconnectedState.value()
            }
        }

        applyBannerFromNetworkState()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                //网络可用
                applyBannerFromNetworkState()
            }

            override fun onLost(network: Network) {
                //网络断开
                applyBannerFromNetworkState()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                //网络能力变化，如 WiFi 切移动数据
                applyBannerFromNetworkState()
            }
        }
        //只监听具备 `NET_CAPABILITY_INTERNET` 能力的网络（能上网才算，纯局域网不算）
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        //注册网络状态监听器
        connectivityManager.registerNetworkCallback(
            request,
            callback
        )
        val pollJob = scope.launch {
            //调试轮询兜底
            while (isActive) {
                //仅调试日志开启时启动
                if (isDebugLoggingEnabled()) {
                    applyBannerFromNetworkState()
                }
                delay(400L)
            }
        }
        onDispose {
            pollJob.cancel()
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
}

/**
 * 网络断开时顶部统一横幅（恢复连接后由 [NetworkConnectivityMonitor] 自动隐藏）。
 */
@Composable
fun NetworkOfflineBanner(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(
                horizontal = UiConstants.PageHorizontalPadding,
                vertical = UiConstants.ListVerticalSpacing,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(id = R.string.base_str_network_offline_banner),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}