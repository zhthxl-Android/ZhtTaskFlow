package com.example.zhttaskflow.nav.router

/**
 * ## 路由拦截链（Navigation 层，S2/S3）
 *
 * 本包处理 **[AppNavigator] 发起跳转之前** 的挂起拦截（深链解析、登录、权限等），
 * 与 ViewModel [com.example.zhttaskflow.base.mvi.BaseUiEffect] **无直接关系**。
 *
 * ### 接入步骤
 *
 * 1. App / 调试壳：`rememberAppRouterInterceptorChain()` 传入 [AppNavHost] 的 `routerInterceptorChain`。
 * 2. 业务 path 保持纯净；RouteHost 在 `navigate` 前按需调用 `RouteAuthMarker` / `RoutePermissionMarker`。
 * 3. 深链：`RouteDeepLinkMarker.wrap(uri)` 后 `navigator.navigate(...)`；映射 `Redirect` 后继续走权限 / 登录链（`target` 需与 RouteHost 标记策略一致）。
 * 4. 扩展自定义拦截器：实现 [RouterInterceptor]，`RouterInterceptorChain.build { add(...) }`，注意 [priority] 排序。
 *
 * **默认 priority**：深链 `200` → 权限 `150`（`LOGIN + 50`）→ 登录 `100`（见 [com.example.zhttaskflow.nav.interceptor.RouterInterceptorPriorities]）。
 *
 * 失败与取消：链返回 `Cancelled` 时经 [RouterInterceptUiBridge] 展示 Snackbar（Error），不走 Toast。
 *
 * MVI 侧跨页跳转由 ViewModel 下发 [com.example.zhttaskflow.base.ext.NavigationUiEffect]，
 * 在 Feature `*RouteHost` 中消费；Snackbar 等由 [com.example.zhttaskflow.base.ext.PresentationUiEffect]
 * 在 Screen 层消费。双 Collector 约定见 [com.example.zhttaskflow.base.ext.UiEffectConsumption]。
 *
 * 总览文档：[com.example.zhttaskflow.nav.doc.NavArchitecture]。
 */

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.example.zhttaskflow.base.ext.LocalLoadingController
import com.example.zhttaskflow.base.ext.LocalSnackbarDispatcher
import com.example.zhttaskflow.base.ui.SnackbarType
import com.example.zhttaskflow.base.ext.hideLoading
import com.example.zhttaskflow.base.ext.showLoading
import com.example.zhttaskflow.base.ext.showSnackbar
import com.example.zhttaskflow.nav.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 路由跳转请求（进入拦截链前由 [com.example.zhttaskflow.nav.AppNavigator] 构造）。
 */
data class RouteRequest(
    val targetRoute: String,
    val isMainTab: Boolean = false,
)

/**
 * 拦截过程可读写扩展位：登录、权限、深链等场景由后续拦截器实现类使用。
 */
class RouteInterceptExtras {
    /** 登录拦截器写入：当前目标是否需要登录态。 */
    var requiresLogin: Boolean = false

    /** 深链拦截器写入：原始 URI 字符串。 */
    var deepLinkUri: String? = null

    /** 权限拦截器写入：权限组 id（见 [com.example.zhttaskflow.nav.interceptor.PermissionGroups]）。 */
    var requiredPermission: String? = null

    /** 自定义透传数据（模块内约定 key）。 */
    val tags: MutableMap<String, Any> = linkedMapOf()
}

/**
 * 单次拦截上下文；[targetRoute] 可被 [RouteInterceptResult.Redirect] 更新。
 */
class RouteInterceptContext(
    request: RouteRequest,
) {
    var targetRoute: String = request.targetRoute
        private set

    val isMainTab: Boolean = request.isMainTab

    val extras: RouteInterceptExtras = RouteInterceptExtras()

    internal fun applyRedirect(route: String) {
        targetRoute = route
    }
}

/**
 * 拦截器决策。
 */
sealed interface RouteInterceptResult {
    /** 继续执行后续拦截器。 */
    data object Proceed : RouteInterceptResult

    /** 重定向到新路由后继续后续拦截器。 */
    data class Redirect(val route: String) : RouteInterceptResult

    /** 中止跳转；[userMessage] 非空时通过 UI 桥展示错误提示。 */
    data class Abort(val userMessage: String? = null) : RouteInterceptResult
}

/**
 * 路由拦截器： [priority] 数值越大越先执行。
 */
interface RouterInterceptor {
    val priority: Int get() = 0

    /** 为 `true` 时拦截链执行期间展示全局 Loading。 */
    val showsLoading: Boolean get() = false

    suspend fun intercept(context: RouteInterceptContext): RouteInterceptResult
}

/** 登录态校验拦截器标记（实现类：[com.example.zhttaskflow.nav.interceptor.LoginInterceptor]）。 */
interface LoginRouteInterceptor : RouterInterceptor

/** 权限申请拦截器标记（实现类：[com.example.zhttaskflow.nav.interceptor.PermissionInterceptor]）。 */
interface PermissionRouteInterceptor : RouterInterceptor

/** 深链解析拦截器标记（实现类：[com.example.zhttaskflow.nav.interceptor.DeepLinkInterceptor]）。 */
interface DeepLinkRouteInterceptor : RouterInterceptor

/**
 * 拦截链执行结果。
 */
sealed interface RouterChainOutcome {
    data class Navigate(val route: String, val isMainTab: Boolean) : RouterChainOutcome

    data class Cancelled(val message: String?) : RouterChainOutcome
}

/**
 * 拦截过程 UI：加载弹窗与失败提示（均经 [BaseScaffold] CompositionLocal；无宿主时安全 no-op，统一走 Snackbar）。
 */
interface RouterInterceptUiBridge {
    fun showLoading(message: String? = null)

    fun hideLoading()

    fun showRouteError(message: String)
}

class RouterInterceptUiBridgeImpl(
    private val showLoadingAction: (String?) -> Unit,
    private val hideLoadingAction: () -> Unit,
    private val showErrorAction: (String) -> Unit,
) : RouterInterceptUiBridge {
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
fun rememberRouterInterceptUiBridge(
    defaultErrorMessage: String = stringResource(id = R.string.nav_str_route_intercept_failed),
): RouterInterceptUiBridge {
    val loadingController = runCatching { LocalLoadingController.current }.getOrNull()
    val snackbarDispatcher = runCatching { LocalSnackbarDispatcher.current }.getOrNull()
    return remember(loadingController, snackbarDispatcher, defaultErrorMessage) {
        RouterInterceptUiBridgeImpl(
            showLoadingAction = { message ->
                loadingController?.let { controller -> showLoading(controller, message) }
            },
            hideLoadingAction = {
                loadingController?.let { controller -> hideLoading(controller) }
            },
            showErrorAction = { message ->
                val dispatcher = snackbarDispatcher ?: return@RouterInterceptUiBridgeImpl
                showSnackbar(
                    dispatcher = dispatcher,
                    message = message.ifBlank { defaultErrorMessage },
                    type = SnackbarType.Error,
                )
            },
        )
    }
}

/**
 * 多拦截器按 [RouterInterceptor.priority] 降序执行；空链时直接放行。
 */
class RouterInterceptorChain internal constructor(
    private val interceptors: List<RouterInterceptor>,
    private val uiBridge: RouterInterceptUiBridge?,
    private val defaultErrorMessage: String,
) {
    val isEmpty: Boolean get() = interceptors.isEmpty()

    suspend fun intercept(request: RouteRequest): RouterChainOutcome {
        if (interceptors.isEmpty()) {
            return RouterChainOutcome.Navigate(
                route = request.targetRoute,
                isMainTab = request.isMainTab,
            )
        }
        val sorted = interceptors.sortedByDescending { interceptor -> interceptor.priority }
        val context = RouteInterceptContext(request)
        val shouldShowLoading = sorted.any { interceptor -> interceptor.showsLoading }
        try {
            if (shouldShowLoading) {
                uiBridge?.showLoading()
            }
            for (interceptor in sorted) {
                when (val result = interceptor.intercept(context)) {
                    RouteInterceptResult.Proceed -> Unit
                    is RouteInterceptResult.Redirect -> context.applyRedirect(result.route)
                    is RouteInterceptResult.Abort -> {
                        return RouterChainOutcome.Cancelled(result.userMessage)
                    }
                }
            }
            return RouterChainOutcome.Navigate(
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
        bridge: RouterInterceptUiBridge,
        defaultErrorMessage: String = this.defaultErrorMessage,
    ): RouterInterceptorChain {
        return RouterInterceptorChain(
            interceptors = interceptors,
            uiBridge = bridge,
            defaultErrorMessage = defaultErrorMessage,
        )
    }

    companion object {
        val Empty: RouterInterceptorChain = RouterInterceptorChain(
            interceptors = emptyList(),
            uiBridge = null,
            defaultErrorMessage = "",
        )

        fun build(
            uiBridge: RouterInterceptUiBridge? = null,
            defaultErrorMessage: String = "",
            block: Builder.() -> Unit,
        ): RouterInterceptorChain {
            return Builder().apply(block).build(uiBridge, defaultErrorMessage)
        }
    }

    class Builder {
        private val items = mutableListOf<RouterInterceptor>()

        fun add(interceptor: RouterInterceptor) {
            items.add(interceptor)
        }

        fun build(
            uiBridge: RouterInterceptUiBridge?,
            defaultErrorMessage: String,
        ): RouterInterceptorChain {
            return RouterInterceptorChain(
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
class RouteRedirectInterceptor(
    private val shouldRedirect: (RouteInterceptContext) -> Boolean,
    private val redirectRoute: String,
    override val priority: Int = 0,
) : RouterInterceptor {
    override suspend fun intercept(context: RouteInterceptContext): RouteInterceptResult {
        return if (shouldRedirect(context)) {
            RouteInterceptResult.Redirect(redirectRoute)
        } else {
            RouteInterceptResult.Proceed
        }
    }
}

/**
 * 测试/示例：满足条件时中止跳转并提示。
 */
class RouteAbortInterceptor(
    private val shouldAbort: (RouteInterceptContext) -> Boolean,
    private val message: String,
    override val priority: Int = 0,
) : RouterInterceptor {
    override suspend fun intercept(context: RouteInterceptContext): RouteInterceptResult {
        return if (shouldAbort(context)) {
            RouteInterceptResult.Abort(userMessage = message)
        } else {
            RouteInterceptResult.Proceed
        }
    }
}

internal fun dispatchRouteNavigation(
    scope: CoroutineScope,
    chain: RouterInterceptorChain,
    request: RouteRequest,
    onNavigate: (RouterChainOutcome.Navigate) -> Unit,
) {
    if (chain.isEmpty) {
        onNavigate(
            RouterChainOutcome.Navigate(
                route = request.targetRoute,
                isMainTab = request.isMainTab,
            ),
        )
        return
    }
    scope.launch {
        when (val outcome = chain.intercept(request)) {
            is RouterChainOutcome.Navigate -> onNavigate(outcome)
            is RouterChainOutcome.Cancelled -> chain.notifyFailure(outcome.message)
        }
    }
}
