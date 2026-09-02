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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
import com.example.zhttaskflow.core.log.TaskFlowLogger
import com.example.zhttaskflow.core.network.NetworkChecker
import com.example.zhttaskflow.core.util.nullIfBlank
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.cancellation.CancellationException

private const val EXCEPTION_LOG_TAG = "Exception"
private const val GLOBAL_EXCEPTION_PAGE_ID = "AppShell"
private const val CRASH_ACTION_ID = "app_uncaught_crash"
private const val BUSINESS_ACTION_ID = "app_business_exception"

/**
 * 崩溃 / 未捕获异常上报抽象：产品环境由壳工程注入友盟、Bugly 等实现。
 */
fun interface TaskFlowCrashReporter {

    /**
     * @param fatal `true` 表示进程级未捕获崩溃；`false` 表示协程等可恢复未捕获异常。
     */
    fun reportCrash(throwable: Throwable, fatal: Boolean)
}

/**
 * 调试默认上报： [TaskFlowLogger.errorAlways] + [com.example.zhttaskflow.base.analytics.TaskFlowAnalytics] outcome。
 */
object TaskFlowDebugCrashReporter : TaskFlowCrashReporter {

    override fun reportCrash(throwable: Throwable, fatal: Boolean) {
        val scene = if (fatal) "fatal" else "non_fatal"
        TaskFlowLogger.errorAlways(EXCEPTION_LOG_TAG, {
            "[$scene] ${throwable.message.nullIfBlank() ?: throwable::class.simpleName.orEmpty()}"
        }, throwable)
        TaskFlowAnalyticsRegistry.current().trackUiOutcome(
            outcome = "failure",
            operationId = CRASH_ACTION_ID,
            pageId = GLOBAL_EXCEPTION_PAGE_ID,
            params = mapOf(
                "fatal" to fatal.toString(),
                "type" to throwable::class.simpleName.orEmpty(),
            ),
        )
    }
}

val LocalTaskFlowCrashReporter = staticCompositionLocalOf<TaskFlowCrashReporter> {
    TaskFlowDebugCrashReporter
}

/**
 * 全局异常与网络离线监控入口（日志、Analytics、Snackbar、顶部横幅）。
 *
 * - **业务异常**（含 [TaskFlowException] 及普通 [Exception]）：友好文案走全局 Snackbar，不替代 ViewModel 既有 `onError`。
 * - **崩溃级**（[Error] 及线程未捕获）：仅上报 + 日志，交还系统默认处理器。
 */
object TaskFlowExceptionHandler {

    private const val LOG_TAG = EXCEPTION_LOG_TAG

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
            pageId = GLOBAL_EXCEPTION_PAGE_ID,
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
        crashReporter: TaskFlowCrashReporter = TaskFlowDebugCrashReporter,
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
        crashReporter: TaskFlowCrashReporter = TaskFlowDebugCrashReporter,
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
    val resolvedReporter = crashReporter ?: remember { TaskFlowDebugCrashReporter }
    var offlineBannerVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current.applicationContext

    DisposableEffect(resolvedReporter) {
        val resetHandler = TaskFlowExceptionHandler.installUncaughtExceptionHandler(resolvedReporter)
        onDispose { resetHandler() }
    }

    CompositionLocalProvider(LocalTaskFlowCrashReporter provides resolvedReporter) {
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
    DisposableEffect(context) {
        val connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
            ?: return@DisposableEffect onDispose { }
        val initialConnected = NetworkChecker.isNetworkAvailable(context)
        if (!initialConnected) {
            disconnectedState.value()
        } else {
            connectedState.value()
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connectedState.value()
            }

            override fun onLost(network: Network) {
                if (!NetworkChecker.isNetworkAvailable(context)) {
                    disconnectedState.value()
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                if (NetworkChecker.isNetworkAvailable(context)) {
                    connectedState.value()
                } else {
                    disconnectedState.value()
                }
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        onDispose {
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
