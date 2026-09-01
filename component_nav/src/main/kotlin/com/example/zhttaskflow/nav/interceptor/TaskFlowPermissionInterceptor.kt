package com.example.zhttaskflow.nav.interceptor

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.zhttaskflow.nav.router.TaskFlowPermissionRouteInterceptor
import com.example.zhttaskflow.nav.router.TaskFlowRouteInterceptContext
import com.example.zhttaskflow.nav.router.TaskFlowRouteInterceptResult
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 路由权限组标记：在 path 后追加 `permissionGroup={id}`，拦截器申请对应权限组后剥离 query 再导航。
 *
 * 与 [TaskFlowRouteAuthMarker] 类似，避免 Navigation 无法识别带 query 的 route 时，应在拦截链通过后得到「干净 path」。
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
 * 权限组 → Manifest 权限列表（样板表；业务可扩展新 group 或替换 [TaskFlowPermissionGrantChecker]）。
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
 * 权限授予状态查询（可替换为正式权限 SDK / 仓库实现）。
 */
interface TaskFlowPermissionGrantChecker {
    fun permissionsForGroup(group: String): List<String>

    fun isGranted(permission: String): Boolean
}

class TaskFlowPermissionGrantCheckerImpl(
    private val context: Context,
    private val groupResolver: (String) -> List<String> = TaskFlowPermissionGroups::permissionsForGroup,
) : TaskFlowPermissionGrantChecker {

    override fun permissionsForGroup(group: String): List<String> {
        return groupResolver(group)
    }

    override fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Composable
fun rememberTaskFlowPermissionGrantChecker(): TaskFlowPermissionGrantChecker {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        TaskFlowPermissionGrantCheckerImpl(context = context)
    }
}

/**
 * 权限申请 UI：挂起直到用户授权或拒绝（样板使用 Activity Result API）。
 */
fun interface TaskFlowPermissionInterceptUi {
    suspend fun requestPermissions(permissions: List<String>): Boolean
}

@Composable
fun rememberTaskFlowPermissionInterceptUi(
    grantChecker: TaskFlowPermissionGrantChecker = rememberTaskFlowPermissionGrantChecker(),
): TaskFlowPermissionInterceptUi {
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    val pendingRequest = remember { PermissionRequestSlot() }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val continuation = pendingRequest.takeContinuation() ?: return@rememberLauncherForActivityResult
        val granted = results.isNotEmpty() && results.values.all { isGranted -> isGranted }
        continuation.resume(granted)
    }
    return remember(activity, grantChecker, launcher, pendingRequest) {
        TaskFlowPermissionInterceptUiImpl(
            activity = activity,
            grantChecker = grantChecker,
            launcher = { permissions -> launcher.launch(permissions.toTypedArray()) },
            pendingRequest = pendingRequest,
        )
    }
}

/**
 * 权限路由拦截器：识别 [TaskFlowRoutePermissionMarker]，未授权时申请权限，成功后重定向至干净 path 并继续后续拦截器。
 */
class TaskFlowPermissionInterceptor(
    private val grantChecker: TaskFlowPermissionGrantChecker,
    private val permissionUi: TaskFlowPermissionInterceptUi,
    private val permissionDeniedMessage: String,
    override val priority: Int = TaskFlowRouterInterceptorPriorities.PERMISSION,
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
            // 未注册的权限组：剥离 query 继续，避免阻塞业务（正式项目可改为 Abort 并打日志）
            return redirectIfNeeded(parsed.cleanRoute, context.targetRoute)
        }

        val allGranted = permissions.all { permission -> grantChecker.isGranted(permission) }
        if (allGranted) {
            return redirectIfNeeded(parsed.cleanRoute, context.targetRoute)
        }

        val granted = permissionUi.requestPermissions(permissions)
        if (!granted) {
            return TaskFlowRouteInterceptResult.Abort(userMessage = permissionDeniedMessage)
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
    private val activity: ComponentActivity?,
    private val grantChecker: TaskFlowPermissionGrantChecker,
    private val launcher: (List<String>) -> Unit,
    private val pendingRequest: PermissionRequestSlot,
) : TaskFlowPermissionInterceptUi {

    override suspend fun requestPermissions(permissions: List<String>): Boolean {
        if (permissions.isEmpty()) {
            return true
        }
        if (permissions.all { permission -> grantChecker.isGranted(permission) }) {
            return true
        }
        if (activity == null) {
            return false
        }
        return suspendCancellableCoroutine { continuation ->
            if (!pendingRequest.setContinuation(continuation)) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            continuation.invokeOnCancellation {
                pendingRequest.takeContinuation()
            }
            launcher(permissions)
        }
    }
}

private class PermissionRequestSlot {
    @Volatile
    private var continuation: CancellableContinuation<Boolean>? = null

    @Synchronized
    fun setContinuation(cont: CancellableContinuation<Boolean>): Boolean {
        if (continuation != null) {
            return false
        }
        continuation = cont
        return true
    }

    @Synchronized
    fun takeContinuation(): CancellableContinuation<Boolean>? {
        val current = continuation
        continuation = null
        return current
    }
}

private tailrec fun Context.findComponentActivity(): ComponentActivity? {
    return when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
}
