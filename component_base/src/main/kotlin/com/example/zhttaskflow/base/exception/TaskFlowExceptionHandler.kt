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
import com.example.zhttaskflow.base.analytics.TaskFlowAnalyticsRegistry
import com.example.zhttaskflow.base.analytics.trackUiOutcome
import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.ext.TaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.mvi.BaseViewModel
import com.example.zhttaskflow.base.ui.TaskFlowUiConstants
import com.example.zhttaskflow.core.foundation.userDisplayMessage
import com.example.zhttaskflow.core.debug.TaskFlowDeveloperTools
import com.example.zhttaskflow.core.log.TaskFlowLogger
import com.example.zhttaskflow.core.network.NetworkChecker
import com.example.zhttaskflow.core.util.isTaskFlowDebugLoggingEnabled
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
 * - **业务异常**（含 [com.example.zhttaskflow.core.foundation.TaskFlowException] 及普通 [Exception]）：友好文案走全局 Snackbar，不替代 ViewModel 既有 `onError`。
 * - **崩溃级**（[Error] 及线程未捕获）：仅上报 + 日志，交还系统默认处理器。
 */
object TaskFlowExceptionHandler {

    private const val LOG_TAG = TASKFLOW_CRASH_LOG_TAG

    /**
     * 已捕获的业务向异常：展示 Error Snackbar 并写日志（不触发进程退出）。
     */
    fun handleBusinessException(
        throwable: Throwable,
        snackbarDispatcher: TaskFlowSnackbarDispatcher?,
        userMessageFallback: String = BaseViewModel.DEFAULT_USER_MESSAGE_FALLBACK,
    ) {
        if (throwable is CancellationException) {
            return
        }
        if (isCrashThrowable(throwable)) {
            reportCrash(throwable, fatal = false)
            return
        }
        val userMessage = throwable.userDisplayMessage(userMessageFallback)
        TaskFlowLogger.errorAlways(LOG_TAG, { "business: $userMessage" }, throwable)
        TaskFlowAnalyticsRegistry.current().trackUiOutcome(
            outcome = "failure",
            operationId = BUSINESS_ACTION_ID,
            pageId = TASKFLOW_CRASH_PAGE_ID,
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
        snackbarDispatcher: TaskFlowSnackbarDispatcher?,
        userMessageFallback: String = BaseViewModel.DEFAULT_USER_MESSAGE_FALLBACK,
        crashReporter: TaskFlowCrashReporter = TaskFlowCrashReporterRegistry.current(),
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

    fun reportCrash(
        throwable: Throwable,
        fatal: Boolean,
        crashReporter: TaskFlowCrashReporter = TaskFlowCrashReporterRegistry.current(),
    ) {
        crashReporter.reportCrash(throwable, fatal)
    }

    internal fun isCrashThrowable(throwable: Throwable): Boolean {
        return throwable is Error
    }

    internal fun installUncaughtExceptionHandler(
        crashReporter: TaskFlowCrashReporter,
    ): () -> Unit {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        val handler = Thread.UncaughtExceptionHandler { thread, exception ->
            crashReporter.reportCrash(exception, fatal = true)
            TaskFlowLogger.errorAlways(LOG_TAG, {
                "uncaught thread=${thread.name} ${exception.message.nullIfBlank().orEmpty()}"
            }, exception)
            previous?.uncaughtException(thread, exception)
        }
        Thread.setDefaultUncaughtExceptionHandler(handler)
        return { Thread.setDefaultUncaughtExceptionHandler(previous) }
    }
}

/**
 * 壳层装配：注入 [LocalTaskFlowCrashReporter]、安装未捕获异常钩子、监听网络并在断开时展示顶部横幅。
 *
 * 由 [com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold] 在拥有全局宿主时调用；`analyticsImpl` 可经
 * [com.example.zhttaskflow.base.analytics.TaskFlowAnalyticsCompositionRoot] 在外层已注入。
 */
@Composable
fun TaskFlowExceptionMonitoringRoot(
    snackbarDispatcher: TaskFlowSnackbarDispatcher?,
    crashReporter: TaskFlowCrashReporter? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val resolvedReporter = rememberTaskFlowCrashReporter(override = crashReporter)
    var offlineBannerVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current.applicationContext

    DisposableEffect(resolvedReporter) {
        val resetHandler = TaskFlowExceptionHandler.installUncaughtExceptionHandler(resolvedReporter)
        onDispose { resetHandler() }
    }

    TaskFlowCrashReporterCompositionRoot(crashReporter = resolvedReporter) {
        TaskFlowNetworkConnectivityMonitor(
            context = context,
            onDisconnected = { offlineBannerVisible = true },
            onConnected = { offlineBannerVisible = false },
        )
        Box(modifier = modifier.fillMaxSize()) {
            content()
            if (offlineBannerVisible) {
                TaskFlowNetworkOfflineBanner(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TaskFlowNetworkConnectivityMonitor(
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
            if (TaskFlowDeveloperTools.shouldForceOfflineBanner()) {
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
                if (isTaskFlowDebugLoggingEnabled()) {
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
 * 网络断开时顶部统一横幅（恢复连接后由 [TaskFlowNetworkConnectivityMonitor] 自动隐藏）。
 */
@Composable
fun TaskFlowNetworkOfflineBanner(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(
                horizontal = TaskFlowUiConstants.PageHorizontalPadding,
                vertical = TaskFlowUiConstants.ListVerticalSpacing,
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
