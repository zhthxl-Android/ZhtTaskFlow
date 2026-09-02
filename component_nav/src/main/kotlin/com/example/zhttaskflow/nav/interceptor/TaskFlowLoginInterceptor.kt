package com.example.zhttaskflow.nav.interceptor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.example.zhttaskflow.base.ext.LocalTaskFlowDialogController
import com.example.zhttaskflow.base.ext.LocalTaskFlowLoadingController
import com.example.zhttaskflow.base.ext.LocalTaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ext.TaskFlowDialogController
import com.example.zhttaskflow.base.ext.TaskFlowLoadingController
import com.example.zhttaskflow.base.ext.TaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ext.hideLoading
import com.example.zhttaskflow.base.ext.showConfirmDialog
import com.example.zhttaskflow.base.ext.showLoading
import com.example.zhttaskflow.base.ext.showSnackbar
import com.example.zhttaskflow.base.ui.TaskFlowSnackbarType
import com.example.zhttaskflow.nav.R
import com.example.zhttaskflow.nav.router.TaskFlowLoginRouteInterceptor
import com.example.zhttaskflow.nav.router.TaskFlowRouteInterceptContext
import com.example.zhttaskflow.nav.router.TaskFlowRouteInterceptResult
import com.example.zhttaskflow.nav.router.TaskFlowRouterInterceptorChain
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
object TaskFlowRouteAuthMarker {
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
class TaskFlowLoginSession {
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
fun rememberTaskFlowLoginSession(): TaskFlowLoginSession {
    return remember { TaskFlowLoginSession() }
}

/**
 * 登录引导 UI：确认弹窗 + 全局 Loading + Snackbar，与 [TaskFlowBaseScaffold] CompositionLocal 对齐。
 */
fun interface TaskFlowLoginInterceptUi {
    suspend fun requestLogin(): Boolean
}

/**
 * 默认应用级拦截链：**深链 `200` → 权限 `150` → 登录 `100`**（按 [TaskFlowRouterInterceptorPriorities] 与各类 [priority] **降序**执行）。
 *
 * 壳层可替换默认依赖而无需改 Feature：
 * - [loginSession]：登录态与模拟登录会话
 * - [deepLinkRouteMapper]：运营 URL → 内部 path（含带门禁 query 的 `target`）
 * - [permissionGrantChecker]：权限组是否已授权（可接系统 Permission API）
 *
 * @see com.example.zhttaskflow.nav.doc.TaskFlowNavArchitecture
 */
@Composable
fun rememberTaskFlowAppRouterInterceptorChain(
    loginSession: TaskFlowLoginSession = rememberTaskFlowLoginSession(),
    deepLinkRouteMapper: TaskFlowDeepLinkRouteMapper = rememberTaskFlowDeepLinkRouteMapper(),
    permissionGrantChecker: TaskFlowPermissionGrantChecker = rememberTaskFlowPermissionGrantChecker(),
): TaskFlowRouterInterceptorChain {
    val loginUi = rememberTaskFlowLoginInterceptUi(loginSession = loginSession)
    val permissionUi = rememberTaskFlowPermissionInterceptUi(grantChecker = permissionGrantChecker)
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
        TaskFlowRouterInterceptorChain.build {
            add(
                TaskFlowDeepLinkInterceptor(
                    routeMapper = deepLinkRouteMapper,
                    parseErrorMessage = deepLinkParseError,
                    unmappedErrorMessage = deepLinkUnmappedError,
                ),
            )
            add(
                TaskFlowLoginInterceptor(
                    loginSession = loginSession,
                    loginUi = loginUi,
                    priority = TaskFlowRouterInterceptorPriorities.LOGIN,
                ),
            )
            add(
                TaskFlowPermissionInterceptor(
                    grantChecker = permissionGrantChecker,
                    permissionUi = permissionUi,
                    permissionDeniedMessage = permissionDeniedMessage,
                ),
            )
        }
    }
}

@Composable
fun rememberTaskFlowLoginInterceptUi(
    loginSession: TaskFlowLoginSession,
): TaskFlowLoginInterceptUi {
    val scope = rememberCoroutineScope()
    val dialogController = runCatching { LocalTaskFlowDialogController.current }.getOrNull()
    val loadingController = runCatching { LocalTaskFlowLoadingController.current }.getOrNull()
    val snackbarDispatcher = runCatching { LocalTaskFlowSnackbarDispatcher.current }.getOrNull()
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
        TaskFlowLoginInterceptUiImpl(
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
 * 登录态路由拦截器：识别 [TaskFlowRouteAuthMarker] 与可选路由表，未登录时引导登录并支持登录后继续原跳转。
 */
class TaskFlowLoginInterceptor(
    private val loginSession: TaskFlowLoginSession,
    private val loginUi: TaskFlowLoginInterceptUi,
    private val routesRequiringLogin: Set<String> = emptySet(),
    override val priority: Int = 100,
    override val showsLoading: Boolean = false,
) : TaskFlowLoginRouteInterceptor {

    override suspend fun intercept(context: TaskFlowRouteInterceptContext): TaskFlowRouteInterceptResult {
        val parsed = TaskFlowRouteAuthMarker.parse(context.targetRoute)
        val requiresLogin = parsed.requiresLogin ||
            routesRequiringLogin.any { pattern -> parsed.cleanRoute.startsWith(pattern) }

        context.extras.requiresLogin = requiresLogin

        if (!requiresLogin) {
            return if (parsed.cleanRoute != context.targetRoute) {
                TaskFlowRouteInterceptResult.Redirect(parsed.cleanRoute)
            } else {
                TaskFlowRouteInterceptResult.Proceed
            }
        }

        if (!loginSession.isLoggedIn) {
            val loggedIn = loginUi.requestLogin()
            if (!loggedIn) {
                return TaskFlowRouteInterceptResult.Abort(userMessage = null)
            }
        }

        return if (parsed.cleanRoute != context.targetRoute) {
            TaskFlowRouteInterceptResult.Redirect(parsed.cleanRoute)
        } else {
            TaskFlowRouteInterceptResult.Proceed
        }
    }
}

private class TaskFlowLoginInterceptUiImpl(
    private val scope: CoroutineScope,
    private val loginSession: TaskFlowLoginSession,
    private val dialogController: TaskFlowDialogController?,
    private val loadingController: TaskFlowLoadingController?,
    private val snackbarDispatcher: TaskFlowSnackbarDispatcher?,
    private val title: String,
    private val message: String,
    private val confirmText: String,
    private val dismissText: String,
    private val successMessage: String,
    private val cancelledMessage: String,
) : TaskFlowLoginInterceptUi {

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
                        type = TaskFlowSnackbarType.Normal,
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
                    type = TaskFlowSnackbarType.Success,
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
