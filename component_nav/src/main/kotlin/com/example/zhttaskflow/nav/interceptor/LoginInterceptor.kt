package com.example.zhttaskflow.nav.interceptor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.example.zhttaskflow.base.ext.LocalDialogController
import com.example.zhttaskflow.base.ext.LocalLoadingController
import com.example.zhttaskflow.base.ext.LocalSnackbarDispatcher
import com.example.zhttaskflow.base.ext.DialogController
import com.example.zhttaskflow.base.ext.LoadingController
import com.example.zhttaskflow.base.ext.SnackbarDispatcher
import com.example.zhttaskflow.base.ext.hideLoading
import com.example.zhttaskflow.base.ext.showConfirmDialog
import com.example.zhttaskflow.base.ext.showLoading
import com.example.zhttaskflow.base.ext.showSnackbar
import com.example.zhttaskflow.base.ui.SnackbarType
import com.example.zhttaskflow.nav.R
import com.example.zhttaskflow.nav.router.LoginRouteInterceptor
import com.example.zhttaskflow.nav.router.RouteInterceptContext
import com.example.zhttaskflow.nav.router.RouteInterceptResult
import com.example.zhttaskflow.nav.router.RouterInterceptorChain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 路由登录标记：在 path 后追加 `?needLogin=true`（或 `&needLogin=true`）表示目标页需登录态。
 *
 * 拦截器会在真正导航前剥离该查询参数，避免 Navigation 无法识别带 query 的 route。
 */
object RouteAuthMarker {
    const val QUERY_NEED_LOGIN: String = "needLogin"

    data class ParsedRoute(
        val cleanRoute: String,
        val requiresLogin: Boolean,
    )

    /**
     * 为样板/测试构造带登录标记的导航 path（不改变业务侧默认无标记跳转）。
     */
    fun withNeedLogin(route: String): String {
        val separator = if (route.contains('?')) "&" else "?"
        return "$route$separator$QUERY_NEED_LOGIN=true"
    }

    fun parse(route: String): ParsedRoute {
        val queryIndex = route.indexOf('?')
        if (queryIndex < 0) {
            return ParsedRoute(cleanRoute = route, requiresLogin = false)
        }
        val path = route.substring(0, queryIndex)
        val query = route.substring(queryIndex + 1)
        val requiresLogin = query.split('&').any { segment ->
            val keyValue = segment.split('=', limit = 2)
            val key = keyValue.firstOrNull().orEmpty()
            val value = keyValue.getOrNull(1)
            key == QUERY_NEED_LOGIN && (value.isNullOrEmpty() || value == "true" || value == "1")
        }
        return ParsedRoute(cleanRoute = path, requiresLogin = requiresLogin)
    }
}

/**
 * 壳工程可替换的登录态持有（样板：内存布尔；正式接入可换 DataStore / Token 仓库）。
 */
@Stable
class LoginSession {
    @Volatile
    var isLoggedIn: Boolean = false
        private set

    fun markLoggedIn() {
        isLoggedIn = true
    }

    fun markLoggedOut() {
        isLoggedIn = false
    }
}

@Composable
fun rememberLoginSession(): LoginSession {
    val injected = LocalLoginSession.current
    if (injected != null) {
        return injected
    }
    return remember { LoginSession() }
}

/**
 * 登录引导 UI：确认弹窗 + 全局 Loading + Snackbar，与 [BaseScaffold] CompositionLocal 对齐。
 */
fun interface LoginInterceptUi {
    suspend fun requestLogin(): Boolean
}

/**
 * 默认应用级拦截链：**深链 `200` → 权限 `150` → 登录 `100`**（按 [RouterInterceptorPriorities] 与各类 [priority] **降序**执行）。
 *
 * 壳层可替换默认依赖而无需改 Feature：
 * - [loginSession]：登录态与模拟登录会话
 * - [deepLinkRouteMapper]：运营 URL → 内部 path（含带门禁 query 的 `target`）
 * - [permissionGrantChecker]：权限组是否已授权（可接系统 Permission API）
 *
 * @see com.example.zhttaskflow.nav.doc.NavArchitecture
 */
@Composable
fun rememberAppRouterInterceptorChain(
    loginSession: LoginSession = rememberLoginSession(),
    deepLinkRouteMapper: DeepLinkRouteMapper = rememberDeepLinkRouteMapper(),
    permissionGrantChecker: PermissionGrantChecker = rememberPermissionGrantChecker(),
): RouterInterceptorChain {
    val loginUi = rememberLoginInterceptUi(loginSession = loginSession)
    val permissionUi = rememberPermissionInterceptUi(grantChecker = permissionGrantChecker)
    val deepLinkParseError = stringResource(id = R.string.nav_str_deeplink_parse_failed)
    val deepLinkUnmappedError = stringResource(id = R.string.nav_str_deeplink_unmapped)
    val permissionDeniedMessage = stringResource(id = R.string.nav_str_permission_denied)
    return remember(
        loginSession,
        loginUi,
        permissionUi,
        deepLinkRouteMapper,
        permissionGrantChecker,
        deepLinkParseError,
        deepLinkUnmappedError,
        permissionDeniedMessage,
    ) {
        RouterInterceptorChain.build {
            add(
                DeepLinkInterceptor(
                    routeMapper = deepLinkRouteMapper,
                    parseErrorMessage = deepLinkParseError,
                    unmappedErrorMessage = deepLinkUnmappedError,
                ),
            )
            add(
                LoginInterceptor(
                    loginSession = loginSession,
                    loginUi = loginUi,
                    priority = RouterInterceptorPriorities.LOGIN,
                ),
            )
            add(
                PermissionInterceptor(
                    grantChecker = permissionGrantChecker,
                    permissionUi = permissionUi,
                    permissionDeniedMessage = permissionDeniedMessage,
                ),
            )
        }
    }
}

@Composable
fun rememberLoginInterceptUi(
    loginSession: LoginSession,
): LoginInterceptUi {
    val scope = rememberCoroutineScope()
    val dialogController = runCatching { LocalDialogController.current }.getOrNull()
    val loadingController = runCatching { LocalLoadingController.current }.getOrNull()
    val snackbarDispatcher = runCatching { LocalSnackbarDispatcher.current }.getOrNull()
    val title = stringResource(id = R.string.nav_str_login_guide_title)
    val message = stringResource(id = R.string.nav_str_login_guide_message)
    val confirmText = stringResource(id = R.string.nav_str_login_confirm)
    val dismissText = stringResource(id = R.string.nav_str_login_cancel)
    val successMessage = stringResource(id = R.string.nav_str_login_success)
    val cancelledMessage = stringResource(id = R.string.nav_str_login_cancelled)
    return remember(
        loginSession,
        scope,
        dialogController,
        loadingController,
        snackbarDispatcher,
        title,
        message,
        confirmText,
        dismissText,
        successMessage,
        cancelledMessage,
    ) {
        LoginInterceptUiImpl(
            scope = scope,
            loginSession = loginSession,
            dialogController = dialogController,
            loadingController = loadingController,
            snackbarDispatcher = snackbarDispatcher,
            title = title,
            message = message,
            confirmText = confirmText,
            dismissText = dismissText,
            successMessage = successMessage,
            cancelledMessage = cancelledMessage,
        )
    }
}

/**
 * 登录态路由拦截器：识别 [RouteAuthMarker] 与可选路由表，未登录时引导登录并支持登录后继续原跳转。
 */
class LoginInterceptor(
    private val loginSession: LoginSession,
    private val loginUi: LoginInterceptUi,
    private val routesRequiringLogin: Set<String> = emptySet(),
    override val priority: Int = 100,
    override val showsLoading: Boolean = false,
) : LoginRouteInterceptor {

    override suspend fun intercept(context: RouteInterceptContext): RouteInterceptResult {
        val parsed = RouteAuthMarker.parse(context.targetRoute)
        val requiresLogin = parsed.requiresLogin ||
            routesRequiringLogin.any { pattern -> parsed.cleanRoute.startsWith(pattern) }

        context.extras.requiresLogin = requiresLogin

        if (!requiresLogin) {
            return if (parsed.cleanRoute != context.targetRoute) {
                RouteInterceptResult.Redirect(parsed.cleanRoute)
            } else {
                RouteInterceptResult.Proceed
            }
        }

        if (!loginSession.isLoggedIn) {
            val loggedIn = loginUi.requestLogin()
            if (!loggedIn) {
                return RouteInterceptResult.Abort(userMessage = null)
            }
        }

        return if (parsed.cleanRoute != context.targetRoute) {
            RouteInterceptResult.Redirect(parsed.cleanRoute)
        } else {
            RouteInterceptResult.Proceed
        }
    }
}

private class LoginInterceptUiImpl(
    private val scope: CoroutineScope,
    private val loginSession: LoginSession,
    private val dialogController: DialogController?,
    private val loadingController: LoadingController?,
    private val snackbarDispatcher: SnackbarDispatcher?,
    private val title: String,
    private val message: String,
    private val confirmText: String,
    private val dismissText: String,
    private val successMessage: String,
    private val cancelledMessage: String,
) : LoginInterceptUi {

    override suspend fun requestLogin(): Boolean = suspendCancellableCoroutine { continuation ->
        val controller = dialogController
        if (controller == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        showConfirmDialog(
            controller = controller,
            title = title,
            message = message,
            confirmText = confirmText,
            dismissText = dismissText,
            onConfirm = {
                scope.launch {
                    performMockLogin()
                    controller.dismissAll()
                    if (continuation.isActive) {
                        continuation.resume(true)
                    }
                }
            },
            onDismiss = {
                controller.dismissAll()
                snackbarDispatcher?.let { dispatcher ->
                    showSnackbar(
                        dispatcher = dispatcher,
                        message = cancelledMessage,
                        type = SnackbarType.Normal,
                    )
                }
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            },
        )
    }

    private suspend fun performMockLogin() {
        loadingController?.let { controller ->
            showLoading(controller, message = null)
        }
        try {
            delay(MOCK_LOGIN_DELAY_MS)
            loginSession.markLoggedIn()
            snackbarDispatcher?.let { dispatcher ->
                showSnackbar(
                    dispatcher = dispatcher,
                    message = successMessage,
                    type = SnackbarType.Success,
                )
            }
        } finally {
            loadingController?.let { controller -> hideLoading(controller) }
        }
    }

    private companion object {
        const val MOCK_LOGIN_DELAY_MS: Long = 400L
    }
}
