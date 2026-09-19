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
    val targetRoute: String,//目标路由路径，支持带参数的完整路径
    val isMainTab: Boolean = false,//是否为底部 Tab 切换，默认 `false`（普通页面跳转）
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
 * 单次拦截流程的上下文对象
 * 贯穿所有拦截器，承载目标路由、Tab 标记、扩展数据；目标路由可被重定向修改
 */
class RouteInterceptContext(
    request: RouteRequest,
) {
    //当前目标路由
    var targetRoute: String = request.targetRoute
        private set

    //是否 Tab 切换
    val isMainTab: Boolean = request.isMainTab

    //扩展数据实例
    val extras: RouteInterceptExtras = RouteInterceptExtras()

    //拦截链执行器调用此方法应用重定向路由
    internal fun applyRedirect(route: String) {
        targetRoute = route
    }
}

/**
 * 拦截器决策。
 * 单个拦截器执行后的决策结果
 */
sealed interface RouteInterceptResult {
    /** 放行，继续执行后续拦截器。 */
    data object Proceed : RouteInterceptResult

    /** 重定向，修改目标路由后，继续执行后续拦截器 */
    data class Redirect(val route: String) : RouteInterceptResult

    /** 中止跳转，整个拦截链终止，可选携带用户可见的错误提示文案 */
    data class Abort(val userMessage: String? = null) : RouteInterceptResult
}

/**
 * 所有路由拦截器的统一抽象接口，自定义拦截器必须实现此接口
 * 路由拦截器： [priority] 数值越大越先执行。
 */
interface RouterInterceptor {
    //拦截器优先级，数值越大越先执行，默认 0
    val priority: Int get() = 0

    /**
     * 执行当前拦截器时，是否需要显示全局 Loading，
     * 默认 `false`；只要链中有一个拦截器为 `true`，整个流程就会显示 Loading */
    val showsLoading: Boolean get() = false

    //核心拦截方法，接收上下文，返回决策结果
    suspend fun intercept(context: RouteInterceptContext): RouteInterceptResult
}

/** 登录态校验拦截器标记（实现类：[com.example.zhttaskflow.nav.interceptor.LoginInterceptor]）。 */
interface LoginRouteInterceptor : RouterInterceptor

/** 权限申请拦截器标记（实现类：[com.example.zhttaskflow.nav.interceptor.PermissionInterceptor]）。 */
interface PermissionRouteInterceptor : RouterInterceptor

/** 深链解析拦截器标记（实现类：[com.example.zhttaskflow.nav.interceptor.DeepLinkInterceptor]）。 */
interface DeepLinkRouteInterceptor : RouterInterceptor

/**
 * 整条拦截链执行完毕后的最终结果
 */
sealed interface RouterChainOutcome {
    //全部拦截通过，执行跳转，携带最终路由和 Tab 标记
    data class Navigate(val route: String, val isMainTab: Boolean) : RouterChainOutcome

    //跳转被取消，携带错误提示信息
    data class Cancelled(val message: String?) : RouterChainOutcome
}

/**
 * 拦截层与 UI 层的桥接接口，隔离拦截逻辑和具体 UI 实现，拦截层只依赖接口，不直接依赖 Compose UI 组件
 * 拦截过程 UI：加载弹窗与失败提示（均经 [BaseScaffold] CompositionLocal；无宿主时安全 no-op，统一走 Snackbar）。
 */
interface RouterInterceptUiBridge {
    //显示加载
    fun showLoading(message: String? = null)

    //隐藏加载
    fun hideLoading()

    //显示路由错误
    fun showRouteError(message: String)
}

/**
 * 桥接接口的函数式实现
 * */
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

/**
 * Compose 环境下的 UI 桥构建函数
 * 通过 `CompositionLocal` 获取全局 Loading 控制器和 Snackbar 调度器，生成桥接实例并缓存
 * */
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
 * 拦截链的核心执行器，管理所有拦截器，按优先级排序执行，统一处理 Loading、异常和失败通知
 * 多拦截器按 [RouterInterceptor.priority] 降序执行；空链时直接放行。
 * @param interceptors 拦截器列表
 * @param uiBridge 拦截链 UI 桥，用于显示 Loading 和错误提示
 * @param defaultErrorMessage 默认错误提示文案
 */
class RouterInterceptorChain internal constructor(
    private val interceptors: List<RouterInterceptor>,
    private val uiBridge: RouterInterceptUiBridge?,
    private val defaultErrorMessage: String,
) {
    //判断拦截链是否为空；空链直接放行，不走拦截逻辑，优化性能
    val isEmpty: Boolean get() = interceptors.isEmpty()

    /**
     * **整条拦截链的执行入口**，按优先级降序逐个执行拦截器，返回最终结果
     * */
    suspend fun intercept(request: RouteRequest): RouterChainOutcome {
        //没有拦截器直接返回 Navigate
        if (interceptors.isEmpty()) {
            return RouterChainOutcome.Navigate(
                route = request.targetRoute,
                isMainTab = request.isMainTab,
            )
        }
        //按优先级数值从大到小排序，优先级高的先执行
        val sorted = interceptors.sortedByDescending { interceptor -> interceptor.priority }
        //基于请求生成拦截上下文
        val context = RouteInterceptContext(request)
        //只要有一个拦截器需要 Loading，就开启全局 Loading
        val shouldShowLoading = sorted.any { interceptor -> interceptor.showsLoading }
        try {
            if (shouldShowLoading) {
                uiBridge?.showLoading()
            }
            //循环执行拦截器
            for (interceptor in sorted) {
                when (val result = interceptor.intercept(context)) {
                    //什么都不做，继续下一个
                    RouteInterceptResult.Proceed -> Unit
                    //调用上下文的 `applyRedirect` 更新目标路由，继续下一个
                    is RouteInterceptResult.Redirect -> context.applyRedirect(result.route)
                    //终止整个链，后续拦截器不再执行
                    is RouteInterceptResult.Abort -> {
                        return RouterChainOutcome.Cancelled(result.userMessage)
                    }
                }
            }
            //返回 `Navigate`，携带最终的目标路由
            return RouterChainOutcome.Navigate(
                route = context.targetRoute,
                isMainTab = request.isMainTab,
            )
        } finally {
            //保证 Loading 关闭
            if (shouldShowLoading) {
                uiBridge?.hideLoading()
            }
        }
    }

    /**
     * 跳转失败时（如路由不存在、原生导航抛异常），
     * 对外暴露的错误通知方法，通过 UI 桥弹出错误提示
     * */
    fun notifyFailure(message: String?) {
        val text = message?.takeIf { it.isNotBlank() } ?: defaultErrorMessage
        if (text.isNotBlank()) {
            uiBridge?.showRouteError(text)
        }
    }

    /**
     * 基于现有拦截链，替换 UI 桥和默认错误文案，生成新的拦截链实例
     * 拦截链在 App 层构建好拦截器列表，进入页面后绑定 Compose 环境的 UI 桥。
     * */
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
        //空拦截链单例，无拦截器、无 UI 桥，默认直接放行，作为默认初始值
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

    //构建者模式，方便逐步添加拦截器，最后生成不可变的拦截链实例
    class Builder {
        private val items = mutableListOf<RouterInterceptor>()

        fun add(interceptor: RouterInterceptor) {
            items.add(interceptor)
        }

        //将可变列表转为不可变列表，构造最终的拦截链
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
 * 测试/示例：
 * 通用重定向拦截器，传入判断条件和目标路由，满足条件就重定向
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
 * 测试/示例：
 * 通用中止拦截器，满足条件就取消跳转并弹出提示
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

/**
 * 拦截链的异步调度入口，
 * AppNavigator 调用此函数启动拦截流程，
 * 空链直接同步放行，非空链在协程中异步执行
 *
 * @param scope 调度协程，用于启动异步任务
 * @param chain 拦截链，可能为空
 * @param request 跳转请求，携带目标路由和是否主 Tab 跳转
 * @param onNavigate 跳转成功回调，拦截通过后执行，由 `AppNavigator` 传入，执行真正的页面跳转
 * */
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
