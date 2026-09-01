package com.example.zhttaskflow.nav.interceptor

import android.Manifest
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import com.example.zhttaskflow.base.ext.LocalTaskFlowDialogController
import com.example.zhttaskflow.base.ext.LocalTaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ext.TaskFlowDialogController
import com.example.zhttaskflow.base.ext.TaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ext.showConfirmDialog
import com.example.zhttaskflow.base.ext.showSnackbar
import com.example.zhttaskflow.base.ui.TaskFlowSnackbarType
import com.example.zhttaskflow.nav.R
import com.example.zhttaskflow.nav.router.TaskFlowPermissionRouteInterceptor
import com.example.zhttaskflow.nav.router.TaskFlowRouteInterceptContext
import com.example.zhttaskflow.nav.router.TaskFlowRouteInterceptResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 路由权限组标记：在 path 后追加 `permissionGroup={id}`，拦截器申请对应权限组后剥离 query 再导航。
 *
 * 与 [TaskFlowRouteAuthMarker] 类似，避免 Navigation 无法识别带 query 的 route 时，应在拦截链通过后得到「干净 path」。
 * 可与 `needLogin` 等其它 query 共存（建议权限拦截器优先级高于登录，先剥离本标记）。
 */
object TaskFlowRoutePermissionMarker {
    const val QUERY_PERMISSION_GROUP: String = "permissionGroup"

    data class ParsedRoute(
        val cleanRoute: String,
        val permissionGroup: String?,
    )

    /**
     * 为需权限的页面构造导航 path（业务按需调用，默认跳转可不携带）。
     */
    fun withPermissionGroup(route: String, group: String): String {
        val encodedGroup = Uri.encode(group.trim())
        val separator = if (route.contains('?')) "&" else "?"
        return "$route$separator$QUERY_PERMISSION_GROUP=$encodedGroup"
    }

    /**
     * **业务接入模板**：任务详情等需存储能力的页面使用 [TaskFlowPermissionGroups.STORAGE]。
     */
    fun withStoragePermission(route: String): String {
        return withPermissionGroup(route, TaskFlowPermissionGroups.STORAGE)
    }

    fun parse(route: String): ParsedRoute {
        val queryIndex = route.indexOf('?')
        if (queryIndex < 0) {
            return ParsedRoute(cleanRoute = route, permissionGroup = null)
        }
        val path = route.substring(0, queryIndex)
        val query = route.substring(queryIndex + 1)
        var group: String? = null
        val remainingSegments = mutableListOf<String>()
        query.split('&').forEach { segment ->
            if (segment.isBlank()) {
                return@forEach
            }
            val keyValue = segment.split('=', limit = 2)
            val key = keyValue.firstOrNull().orEmpty()
            val value = keyValue.getOrNull(1)
            if (key == QUERY_PERMISSION_GROUP && !value.isNullOrBlank()) {
                group = Uri.decode(value)
            } else {
                remainingSegments.add(segment)
            }
        }
        val cleanRoute = if (remainingSegments.isEmpty()) {
            path
        } else {
            "$path?${remainingSegments.joinToString("&")}"
        }
        return ParsedRoute(cleanRoute = cleanRoute, permissionGroup = group)
    }
}

/**
 * 权限组 → Manifest 权限列表（样板表；正式环境可替换 [TaskFlowPermissionGrantChecker] 为系统权限查询）。
 */
object TaskFlowPermissionGroups {
    const val CAMERA: String = "camera"
    const val STORAGE: String = "storage"

    fun permissionsForGroup(group: String): List<String> {
        return when (group) {
            CAMERA -> listOf(Manifest.permission.CAMERA)
            STORAGE -> listOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
            else -> emptyList()
        }
    }
}

/**
 * 权限授予状态查询（示范实现为内存模拟；业务可替换为真实 Permission SDK / 仓库）。
 */
interface TaskFlowPermissionGrantChecker {
    fun permissionsForGroup(group: String): List<String>

    fun isGranted(permission: String): Boolean
}

/**
 * 示范用内存会话：模拟「已授权」的权限组，不调用系统 [android.content.pm.PackageManager]。
 */
@Stable
class TaskFlowPermissionDemoSession {
    private val grantedGroups = mutableSetOf<String>()

    fun isGroupGranted(group: String): Boolean {
        return group in grantedGroups
    }

    fun markGroupGranted(group: String) {
        grantedGroups.add(group)
    }
}

/**
 * 模拟权限校验：以权限组 id 作为逻辑令牌，[isGranted] 仅在该组已被 [TaskFlowPermissionDemoSession] 标记时返回 true。
 */
class TaskFlowDemoPermissionGrantChecker(
    val demoSession: TaskFlowPermissionDemoSession,
) : TaskFlowPermissionGrantChecker {

    override fun permissionsForGroup(group: String): List<String> {
        if (TaskFlowPermissionGroups.permissionsForGroup(group).isEmpty()) {
            return emptyList()
        }
        return listOf(group)
    }

    override fun isGranted(permission: String): Boolean {
        return demoSession.isGroupGranted(permission)
    }
}

@Composable
fun rememberTaskFlowPermissionGrantChecker(): TaskFlowPermissionGrantChecker {
    return remember {
        TaskFlowDemoPermissionGrantChecker(demoSession = TaskFlowPermissionDemoSession())
    }
}

/**
 * 权限申请 UI：挂起直到用户在全局确认弹窗中授权或拒绝（示范为模拟授权，不调用 Activity Result API）。
 */
fun interface TaskFlowPermissionInterceptUi {
    suspend fun requestPermissions(permissions: List<String>, permissionGroup: String): Boolean
}

@Composable
fun rememberTaskFlowPermissionInterceptUi(
    grantChecker: TaskFlowPermissionGrantChecker = rememberTaskFlowPermissionGrantChecker(),
): TaskFlowPermissionInterceptUi {
    val scope = rememberCoroutineScope()
    val dialogController = runCatching { LocalTaskFlowDialogController.current }.getOrNull()
    val snackbarDispatcher = runCatching { LocalTaskFlowSnackbarDispatcher.current }.getOrNull()
    val title = stringResource(id = R.string.nav_str_permission_guide_title)
    val message = stringResource(id = R.string.nav_str_permission_guide_message)
    val confirmText = stringResource(id = R.string.nav_str_permission_confirm)
    val dismissText = stringResource(id = R.string.nav_str_permission_cancel)
    val successMessage = stringResource(id = R.string.nav_str_permission_success)
    val deniedMessage = stringResource(id = R.string.nav_str_permission_denied)
    val demoSession = (grantChecker as? TaskFlowDemoPermissionGrantChecker)?.demoSession
    return remember(
        scope,
        grantChecker,
        dialogController,
        snackbarDispatcher,
        demoSession,
        title,
        message,
        confirmText,
        dismissText,
        successMessage,
        deniedMessage,
    ) {
        TaskFlowPermissionInterceptUiImpl(
            scope = scope,
            grantChecker = grantChecker,
            demoSession = demoSession,
            dialogController = dialogController,
            snackbarDispatcher = snackbarDispatcher,
            title = title,
            message = message,
            confirmText = confirmText,
            dismissText = dismissText,
            successMessage = successMessage,
            deniedMessage = deniedMessage,
        )
    }
}

/**
 * 权限路由拦截器：识别 [TaskFlowRoutePermissionMarker]，未授权时弹出引导弹窗，成功后重定向至干净 path 并继续后续拦截器。
 */
class TaskFlowPermissionInterceptor(
    private val grantChecker: TaskFlowPermissionGrantChecker,
    private val permissionUi: TaskFlowPermissionInterceptUi,
    private val permissionDeniedMessage: String,
    override val priority: Int = TaskFlowRouterInterceptorPriorities.LOGIN + 50,
    override val showsLoading: Boolean = false,
) : TaskFlowPermissionRouteInterceptor {

    override suspend fun intercept(context: TaskFlowRouteInterceptContext): TaskFlowRouteInterceptResult {
        val parsed = TaskFlowRoutePermissionMarker.parse(context.targetRoute)
        val group = parsed.permissionGroup

        if (group.isNullOrBlank()) {
            return redirectIfNeeded(parsed.cleanRoute, context.targetRoute)
        }

        context.extras.requiredPermission = group

        val permissions = grantChecker.permissionsForGroup(group)
        if (permissions.isEmpty()) {
            return redirectIfNeeded(parsed.cleanRoute, context.targetRoute)
        }

        val allGranted = permissions.all { permission -> grantChecker.isGranted(permission) }
        if (allGranted) {
            return redirectIfNeeded(parsed.cleanRoute, context.targetRoute)
        }

        val granted = permissionUi.requestPermissions(permissions = permissions, permissionGroup = group)
        if (!granted) {
            return TaskFlowRouteInterceptResult.Abort(userMessage = null)
        }

        return redirectIfNeeded(parsed.cleanRoute, context.targetRoute)
    }

    private fun redirectIfNeeded(
        cleanRoute: String,
        currentRoute: String,
    ): TaskFlowRouteInterceptResult {
        return if (cleanRoute != currentRoute) {
            TaskFlowRouteInterceptResult.Redirect(cleanRoute)
        } else {
            TaskFlowRouteInterceptResult.Proceed
        }
    }
}

private class TaskFlowPermissionInterceptUiImpl(
    private val scope: CoroutineScope,
    private val grantChecker: TaskFlowPermissionGrantChecker,
    private val demoSession: TaskFlowPermissionDemoSession?,
    private val dialogController: TaskFlowDialogController?,
    private val snackbarDispatcher: TaskFlowSnackbarDispatcher?,
    private val title: String,
    private val message: String,
    private val confirmText: String,
    private val dismissText: String,
    private val successMessage: String,
    private val deniedMessage: String,
) : TaskFlowPermissionInterceptUi {

    override suspend fun requestPermissions(
        permissions: List<String>,
        permissionGroup: String,
    ): Boolean = suspendCancellableCoroutine { continuation ->
        if (permissions.isEmpty()) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }
        if (permissions.all { permission -> grantChecker.isGranted(permission) }) {
            continuation.resume(true)
            return@suspendCancellableCoroutine
        }
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
                    performMockGrant(permissionGroup)
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
                        message = deniedMessage,
                        type = TaskFlowSnackbarType.Error,
                    )
                }
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            },
        )
    }

    private suspend fun performMockGrant(permissionGroup: String) {
        delay(MOCK_PERMISSION_DELAY_MS)
        demoSession?.markGroupGranted(permissionGroup)
        snackbarDispatcher?.let { dispatcher ->
            showSnackbar(
                dispatcher = dispatcher,
                message = successMessage,
                type = TaskFlowSnackbarType.Success,
            )
        }
    }

    private companion object {
        const val MOCK_PERMISSION_DELAY_MS: Long = 300L
    }
}
