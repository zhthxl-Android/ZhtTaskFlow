package com.example.zhttaskflow.base.exception

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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.core.content.ContextCompat
import com.example.zhttaskflow.base.R
import com.example.zhttaskflow.base.analytics.AnalyticsRegistry
import com.example.zhttaskflow.base.analytics.trackUiOutcome
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.ext.SnackbarDispatcher
import com.example.zhttaskflow.base.mvi.BaseViewModel
import com.example.zhttaskflow.base.ui.UiConstants
import com.example.zhttaskflow.core.foundation.userDisplayMessage
import com.example.zhttaskflow.core.debug.DeveloperTools
import com.example.zhttaskflow.core.log.Logger
import com.example.zhttaskflow.core.network.NetworkChecker
import com.example.zhttaskflow.core.util.isDebugLoggingEnabled
import com.example.zhttaskflow.core.util.nullIfBlank
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.coroutines.cancellation.CancellationException

private const val BUSINESS_ACTION_ID = "app_business_exception"

/**
 * 全局异常与网络离线监控入口（日志、Analytics、Snackbar、顶部横幅）。
 *
 * - **业务异常**（含 [com.example.zhttaskflow.core.foundation.AppException] 及普通 [Exception]）：友好文案走全局 Snackbar，不替代 ViewModel 既有 `onError`。
 * - **崩溃级**（[Error] 及线程未捕获）：仅上报 + 日志，交还系统默认处理器。
 */
object ExceptionHandler {

    private const val LOG_TAG = CRASH_LOG_TAG

    /**
     * 已捕获的业务向异常：展示 Error Snackbar 并写日志（不触发进程退出）。
     */
    fun handleBusinessException(
        throwable: Throwable,
        snackbarDispatcher: SnackbarDispatcher?,
        userMessageFallback: String = BaseViewModel.DEFAULT_USER_MESSAGE_FALLBACK,
    ) {
        // 协程取消，忽略
        if (throwable is CancellationException) {
            return
        }
        // is Error
        if (isCrashThrowable(throwable)) {
            // 静默上报，不弹 Snackbar
            reportCrash(throwable, fatal = false)
            return
        }
        // 是 Exception：走业务异常流程
        val userMessage = throwable.userDisplayMessage(userMessageFallback)
        Logger.errorAlways(LOG_TAG, { "business: $userMessage" }, throwable)
        AnalyticsRegistry.current().trackUiOutcome(
            outcome = "failure",
            operationId = BUSINESS_ACTION_ID,
            pageId = CRASH_PAGE_ID,
            params = mapOf("type" to throwable::class.simpleName.orEmpty()),
        )
        snackbarDispatcher?.showSnackbar(
            message = userMessage,
            type = SnackbarType.Error,
        )
    }

    /**
     * 供根 [kotlinx.coroutines.CoroutineScope] 可选挂载（不修改 [BaseViewModel.launchTask] 既有逻辑）。
     */
    fun coroutineExceptionHandler(
        snackbarDispatcher: SnackbarDispatcher?,
        userMessageFallback: String = BaseViewModel.DEFAULT_USER_MESSAGE_FALLBACK,
        crashReporter: CrashReporter = CrashReporterRegistry.current(),
    ): CoroutineExceptionHandler {
        return CoroutineExceptionHandler { _, throwable ->
            if (isCrashThrowable(throwable)) {
                reportCrash(throwable, fatal = false, crashReporter = crashReporter)
            } else {
                handleBusinessException(
                    throwable = throwable,
                    snackbarDispatcher = snackbarDispatcher,
                    userMessageFallback = userMessageFallback,
                )
            }
        }
    }

    // 崩溃上报：简单委托
    fun reportCrash(
        throwable: Throwable,
        fatal: Boolean,
        crashReporter: CrashReporter = CrashReporterRegistry.current(),
    ) {
        crashReporter.reportCrash(throwable, fatal)
    }

    // 判断是不是崩溃级：只认 Error，不认 Exception
    internal fun isCrashThrowable(throwable: Throwable): Boolean {
        return throwable is Error
    }

    // 安装线程未捕获异常处理器
    internal fun installUncaughtExceptionHandler(
        crashReporter: CrashReporter,
    ): () -> Unit {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        val handler = Thread.UncaughtExceptionHandler { thread, exception ->
            crashReporter.reportCrash(exception, fatal = true)
            Logger.errorAlways(LOG_TAG, {
                "uncaught thread=${thread.name} ${exception.message.nullIfBlank().orEmpty()}"
            }, exception)
            //自定义处理器处理完后，必须调用前一个处理器（最终是系统默认），否则用户看不到 "应用已停止" 对话框
            previous?.uncaughtException(thread, exception)
        }
        Thread.setDefaultUncaughtExceptionHandler(handler)
        return { Thread.setDefaultUncaughtExceptionHandler(previous) }// 返回恢复函数
    }
}

/**
 * 壳层装配：注入 [LocalCrashReporter]、安装未捕获异常钩子、监听网络并在断开时展示顶部横幅。
 *
 * 由 [com.example.zhttaskflow.base.ui.BaseScaffold] 在拥有全局宿主时调用；`analyticsImpl` 可经
 * [com.example.zhttaskflow.base.analytics.AnalyticsCompositionRoot] 在外层已注入。
 */
@Composable
fun ExceptionMonitoringRoot(
    snackbarDispatcher: SnackbarDispatcher?,
    crashReporter: CrashReporter? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val resolvedReporter = rememberCrashReporter(override = crashReporter)
    var offlineBannerVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current.applicationContext

    DisposableEffect(resolvedReporter) {
        val resetHandler = ExceptionHandler.installUncaughtExceptionHandler(resolvedReporter)
        onDispose { resetHandler() }
    }

    CrashReporterCompositionRoot(crashReporter = resolvedReporter) {
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
}

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
        val connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
            ?: return@DisposableEffect onDispose { }

        fun applyBannerFromNetworkState() {
            if (DeveloperTools.shouldForceOfflineBanner()) {
                disconnectedState.value()
                return
            }
            if (NetworkChecker.isNetworkAvailable(context)) {
                connectedState.value()
            } else {
                disconnectedState.value()
            }
        }

        applyBannerFromNetworkState()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                applyBannerFromNetworkState()
            }

            override fun onLost(network: Network) {
                applyBannerFromNetworkState()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                applyBannerFromNetworkState()
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        val pollJob = scope.launch {
            while (isActive) {
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
