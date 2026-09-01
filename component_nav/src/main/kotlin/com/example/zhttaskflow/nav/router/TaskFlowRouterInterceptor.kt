package com.example.zhttaskflow.nav.router

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.example.zhttaskflow.base.ext.LocalTaskFlowLoadingController
import com.example.zhttaskflow.base.ext.LocalTaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ui.TaskFlowSnackbarType
import com.example.zhttaskflow.base.ext.hideLoading
import com.example.zhttaskflow.base.ext.showLoading
import com.example.zhttaskflow.base.ext.showSnackbar
import com.example.zhttaskflow.nav.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 路由跳转请求（进入拦截链前由 [com.example.zhttaskflow.nav.TaskFlowNavigator] 构造）。
 */
data class TaskFlowRouteRequest(
    val targetRoute: String,
    val isMainTab: Boolean = false,
)

/**
 * 拦截过程可读写扩展位：登录、权限、深链等场景由后续拦截器实现类使用。
 */
class TaskFlowRouteInterceptExtras {
    /** 预留：目标路由是否需要登录态。 */
    var requiresLogin: Boolean = false

    /** 预留：深链原始 URI 或 path，供解析拦截器写入/读取。 */
    var deepLinkUri: String? = null

    /** 预留：所需权限标识，供权限拦截器校验。 */
    var requiredPermission: String? = null

    /** 自定义透传数据（模块内约定 key）。 */
    val tags: MutableMap<String, Any> = linkedMapOf()
}

/**
 * 单次拦截上下文；[targetRoute] 可被 [TaskFlowRouteInterceptResult.Redirect] 更新。
 */
class TaskFlowRouteInterceptContext(
    request: TaskFlowRouteRequest,
) {
    var targetRoute: String = request.targetRoute
        private set

    val isMainTab: Boolean = request.isMainTab

    val extras: TaskFlowRouteInterceptExtras = TaskFlowRouteInterceptExtras()

    internal fun applyRedirect(route: String) {
        targetRoute = route
    }
}

/**
 * 拦截器决策。
 */
sealed interface TaskFlowRouteInterceptResult {
    /** 继续执行后续拦截器。 */
    data object Proceed : TaskFlowRouteInterceptResult

    /** 重定向到新路由后继续后续拦截器。 */
    data class Redirect(val route: String) : TaskFlowRouteInterceptResult

    /** 中止跳转；[userMessage] 非空时通过 UI 桥展示错误提示。 */
    data class Abort(val userMessage: String? = null) : TaskFlowRouteInterceptResult
}

/**
 * 路由拦截器： [priority] 数值越大越先执行。
 */
interface TaskFlowRouterInterceptor {
    val priority: Int get() = 0

    /** 为 `true` 时拦截链执行期间展示全局 Loading。 */
    val showsLoading: Boolean get() = false

    suspend fun intercept(context: TaskFlowRouteInterceptContext): TaskFlowRouteInterceptResult
}

/** 预留：登录态校验拦截器标记。 */
interface TaskFlowLoginRouteInterceptor : TaskFlowRouterInterceptor

/** 预留：权限校验拦截器标记。 */
interface TaskFlowPermissionRouteInterceptor : TaskFlowRouterInterceptor

/** 预留：深链参数解析拦截器标记。 */
interface TaskFlowDeepLinkRouteInterceptor : TaskFlowRouterInterceptor

/**
 * 拦截链执行结果。
 */
sealed interface TaskFlowRouterChainOutcome {
    data class Navigate(val route: String, val isMainTab: Boolean) : TaskFlowRouterChainOutcome

    data class Cancelled(val message: String?) : TaskFlowRouterChainOutcome
}

/**
 * 拦截过程 UI：加载弹窗与失败提示（均经 [TaskFlowBaseScaffold] CompositionLocal；无宿主时安全 no-op，不使用系统 Toast）。
 */
interface TaskFlowRouterInterceptUiBridge {
    fun showLoading(message: String? = null)

    fun hideLoading()

    fun showRouteError(message: String)
}

class TaskFlowRouterInterceptUiBridgeImpl(
    private val showLoadingAction: (String?) -> Unit,
    private val hideLoadingAction: () -> Unit,
    private val showErrorAction: (String) -> Unit,
) : TaskFlowRouterInterceptUiBridge {
    override fun showLoading(message: String?) {
        showLoadingAction(message)
    }

    override fun hideLoading() {
        hideLoadingAction()
    }

    override fun showRouteError(message: String) {
        showErrorAction(message)
    }
}

@Composable
fun rememberTaskFlowRouterInterceptUiBridge(
    defaultErrorMessage: String = stringResource(id = R.string.nav_str_route_intercept_failed),
): TaskFlowRouterInterceptUiBridge {
    val loadingController = runCatching { LocalTaskFlowLoadingController.current }.getOrNull()
    val snackbarDispatcher = runCatching { LocalTaskFlowSnackbarDispatcher.current }.getOrNull()
    return remember(loadingController, snackbarDispatcher, defaultErrorMessage) {
        TaskFlowRouterInterceptUiBridgeImpl(
            showLoadingAction = { message ->
                loadingController?.let { controller -> showLoading(controller, message) }
            },
            hideLoadingAction = {
                loadingController?.let { controller -> hideLoading(controller) }
            },
            showErrorAction = { message ->
                val dispatcher = snackbarDispatcher ?: return@TaskFlowRouterInterceptUiBridgeImpl
                showSnackbar(
                    dispatcher = dispatcher,
                    message = message.ifBlank { defaultErrorMessage },
                    type = TaskFlowSnackbarType.Error,
                )
            },
        )
    }
}

/**
 * 多拦截器按 [TaskFlowRouterInterceptor.priority] 降序执行；空链时直接放行。
 */
class TaskFlowRouterInterceptorChain internal constructor(
    private val interceptors: List<TaskFlowRouterInterceptor>,
    private val uiBridge: TaskFlowRouterInterceptUiBridge?,
    private val defaultErrorMessage: String,
) {
    val isEmpty: Boolean get() = interceptors.isEmpty()

    suspend fun intercept(request: TaskFlowRouteRequest): TaskFlowRouterChainOutcome {
        if (interceptors.isEmpty()) {
            return TaskFlowRouterChainOutcome.Navigate(
                route = request.targetRoute,
                isMainTab = request.isMainTab,
            )
        }
        val sorted = interceptors.sortedByDescending { interceptor -> interceptor.priority }
        val context = TaskFlowRouteInterceptContext(request)
        val shouldShowLoading = sorted.any { interceptor -> interceptor.showsLoading }
        try {
            if (shouldShowLoading) {
                uiBridge?.showLoading()
            }
            for (interceptor in sorted) {
                when (val result = interceptor.intercept(context)) {
                    TaskFlowRouteInterceptResult.Proceed -> Unit
                    is TaskFlowRouteInterceptResult.Redirect -> context.applyRedirect(result.route)
                    is TaskFlowRouteInterceptResult.Abort -> {
                        return TaskFlowRouterChainOutcome.Cancelled(result.userMessage)
                    }
                }
            }
            return TaskFlowRouterChainOutcome.Navigate(
                route = context.targetRoute,
                isMainTab = request.isMainTab,
            )
        } finally {
            if (shouldShowLoading) {
                uiBridge?.hideLoading()
            }
        }
    }

    fun notifyFailure(message: String?) {
        val text = message?.takeIf { it.isNotBlank() } ?: defaultErrorMessage
        if (text.isNotBlank()) {
            uiBridge?.showRouteError(text)
        }
    }

    fun withUiBridge(
        bridge: TaskFlowRouterInterceptUiBridge,
        defaultErrorMessage: String = this.defaultErrorMessage,
    ): TaskFlowRouterInterceptorChain {
        return TaskFlowRouterInterceptorChain(
            interceptors = interceptors,
            uiBridge = bridge,
            defaultErrorMessage = defaultErrorMessage,
        )
    }

    companion object {
        val Empty: TaskFlowRouterInterceptorChain = TaskFlowRouterInterceptorChain(
            interceptors = emptyList(),
            uiBridge = null,
            defaultErrorMessage = "",
        )

        fun build(
            uiBridge: TaskFlowRouterInterceptUiBridge? = null,
            defaultErrorMessage: String = "",
            block: Builder.() -> Unit,
        ): TaskFlowRouterInterceptorChain {
            return Builder().apply(block).build(uiBridge, defaultErrorMessage)
        }
    }

    class Builder {
        private val items = mutableListOf<TaskFlowRouterInterceptor>()

        fun add(interceptor: TaskFlowRouterInterceptor) {
            items.add(interceptor)
        }

        fun build(
            uiBridge: TaskFlowRouterInterceptUiBridge?,
            defaultErrorMessage: String,
        ): TaskFlowRouterInterceptorChain {
            return TaskFlowRouterInterceptorChain(
                interceptors = items.toList(),
                uiBridge = uiBridge,
                defaultErrorMessage = defaultErrorMessage,
            )
        }
    }
}

/**
 * 测试/示例：满足条件时重定向路由。
 */
class TaskFlowRouteRedirectInterceptor(
    private val shouldRedirect: (TaskFlowRouteInterceptContext) -> Boolean,
    private val redirectRoute: String,
    override val priority: Int = 0,
) : TaskFlowRouterInterceptor {
    override suspend fun intercept(context: TaskFlowRouteInterceptContext): TaskFlowRouteInterceptResult {
        return if (shouldRedirect(context)) {
            TaskFlowRouteInterceptResult.Redirect(redirectRoute)
        } else {
            TaskFlowRouteInterceptResult.Proceed
        }
    }
}

/**
 * 测试/示例：满足条件时中止跳转并提示。
 */
class TaskFlowRouteAbortInterceptor(
    private val shouldAbort: (TaskFlowRouteInterceptContext) -> Boolean,
    private val message: String,
    override val priority: Int = 0,
) : TaskFlowRouterInterceptor {
    override suspend fun intercept(context: TaskFlowRouteInterceptContext): TaskFlowRouteInterceptResult {
        return if (shouldAbort(context)) {
            TaskFlowRouteInterceptResult.Abort(userMessage = message)
        } else {
            TaskFlowRouteInterceptResult.Proceed
        }
    }
}

internal fun dispatchRouteNavigation(
    scope: CoroutineScope,
    chain: TaskFlowRouterInterceptorChain,
    request: TaskFlowRouteRequest,
    onNavigate: (TaskFlowRouterChainOutcome.Navigate) -> Unit,
) {
    if (chain.isEmpty) {
        onNavigate(
            TaskFlowRouterChainOutcome.Navigate(
                route = request.targetRoute,
                isMainTab = request.isMainTab,
            ),
        )
        return
    }
    scope.launch {
        when (val outcome = chain.intercept(request)) {
            is TaskFlowRouterChainOutcome.Navigate -> onNavigate(outcome)
            is TaskFlowRouterChainOutcome.Cancelled -> chain.notifyFailure(outcome.message)
        }
    }
}
