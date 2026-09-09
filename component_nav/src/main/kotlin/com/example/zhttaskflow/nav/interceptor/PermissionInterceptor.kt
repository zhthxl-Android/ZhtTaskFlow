package com.example.zhttaskflow.nav.interceptor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.zhttaskflow.base.ext.LocalDialogController
import com.example.zhttaskflow.base.ext.LocalSnackbarDispatcher
import com.example.zhttaskflow.base.ext.DialogController
import com.example.zhttaskflow.base.ext.SnackbarDispatcher
import com.example.zhttaskflow.base.ext.showConfirmDialog
import com.example.zhttaskflow.base.ext.showSnackbar
import com.example.zhttaskflow.base.ui.SnackbarType
import com.example.zhttaskflow.nav.R
import com.example.zhttaskflow.nav.router.PermissionRouteInterceptor
import com.example.zhttaskflow.nav.router.RouteInterceptContext
import com.example.zhttaskflow.nav.router.RouteInterceptResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 路由权限组标记：在 path 后追加 `permissionGroup={id}`，拦截器申请对应权限组后剥离 query 再导航。
 *
 * 与 [RouteAuthMarker] 类似，避免 Navigation 无法识别带 query 的 route 时，应在拦截链通过后得到「干净 path」。
 * 可与 `needLogin` 等其它 query 共存；默认链中本拦截器 priority 为 `LOGIN + 50`（150），**先于**登录拦截器（100）执行。
 */
object RoutePermissionMarker {
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
     * **业务接入模板**：任务详情等需存储能力的页面使用 [PermissionGroups.STORAGE]。
     */
    fun withStoragePermission(route: String): String {
        return withPermissionGroup(route, PermissionGroups.STORAGE)
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
 * 权限组 → 系统 [Manifest.permission] 列表（随 API 级别区分存储权限）。
 */
object PermissionGroups {
    const val CAMERA: String = "camera"
    const val STORAGE: String = "storage"

    fun permissionsForGroup(group: String): List<String> {
        return when (group) {
            CAMERA -> listOf(Manifest.permission.CAMERA)
            STORAGE -> storagePermissionsForSdk()
            else -> emptyList()
        }
    }

    private fun storagePermissionsForSdk(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}

/**
 * 权限授予状态查询抽象；默认使用 [SystemPermissionGrantChecker]。
 */
interface PermissionGrantChecker {
    fun permissionsForGroup(group: String): List<String>

    fun isGranted(permission: String): Boolean
}

/**
 * 基于 [ContextCompat.checkSelfPermission] 的系统权限查询。
 */
class SystemPermissionGrantChecker(
    private val appContext: Context,
) : PermissionGrantChecker {

    override fun permissionsForGroup(group: String): List<String> {
        return PermissionGroups.permissionsForGroup(group)
    }

    override fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            permission,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

/**
 * 权限校验实现选择：默认走系统；调试 / 无 Activity 时可切换 [Demo]。
 */
enum class PermissionGrantCheckerMode {
    System,
    Demo,
}

@Composable
fun rememberPermissionGrantChecker(
    mode: PermissionGrantCheckerMode = PermissionGrantCheckerMode.System,
): PermissionGrantChecker {
    val context = LocalContext.current.applicationContext
    return remember(context, mode) {
        when (mode) {
            PermissionGrantCheckerMode.System -> SystemPermissionGrantChecker(context)
            PermissionGrantCheckerMode.Demo -> {
                DemoPermissionGrantChecker(demoSession = PermissionDemoSession())
            }
        }
    }
}

/**
 * 示范用内存会话：模拟「已授权」的权限组，不调用系统 API（[PermissionGrantCheckerMode.Demo] / 降级时使用）。
 */
@Stable
class PermissionDemoSession {
    private val grantedGroups = mutableSetOf<String>()

    fun isGroupGranted(group: String): Boolean {
        return group in grantedGroups
    }

    fun markGroupGranted(group: String) {
        grantedGroups.add(group)
    }
}

/**
 * 模拟权限校验：以权限组 id 作为逻辑令牌，[isGranted] 仅在该组已被 [PermissionDemoSession] 标记时返回 true。
 */
class DemoPermissionGrantChecker(
    val demoSession: PermissionDemoSession,
) : PermissionGrantChecker {

    override fun permissionsForGroup(group: String): List<String> {
        if (PermissionGroups.permissionsForGroup(group).isEmpty()) {
            return emptyList()
        }
        return listOf(group)
    }

    override fun isGranted(permission: String): Boolean {
        return demoSession.isGroupGranted(permission)
    }
}

/**
 * 权限申请 UI：引导弹窗（全局 [com.example.zhttaskflow.base.ext.DialogController]）+ 系统运行时申请或模拟降级。
 */
fun interface PermissionInterceptUi {
    suspend fun requestPermissions(permissions: List<String>, permissionGroup: String): Boolean
}

/**
 * @param grantChecker 默认 [rememberPermissionGrantChecker]（系统查询）。
 * @param forceDemoRequest 为 `true` 时强制模拟授权，不弹出系统权限框。
 */
@Composable
fun rememberPermissionInterceptUi(
    grantChecker: PermissionGrantChecker = rememberPermissionGrantChecker(),
    forceDemoRequest: Boolean = false,
): PermissionInterceptUi {
    val scope = rememberCoroutineScope()
    val dialogController = runCatching { LocalDialogController.current }.getOrNull()
    val snackbarDispatcher = runCatching { LocalSnackbarDispatcher.current }.getOrNull()
    val title = stringResource(id = R.string.nav_str_permission_guide_title)
    val message = stringResource(id = R.string.nav_str_permission_guide_message)
    val confirmText = stringResource(id = R.string.nav_str_permission_confirm)
    val dismissText = stringResource(id = R.string.nav_str_permission_cancel)
    val successMessage = stringResource(id = R.string.nav_str_permission_success)
    val deniedMessage = stringResource(id = R.string.nav_str_permission_denied)
    val demoSession = (grantChecker as? DemoPermissionGrantChecker)?.demoSession
    val activity = LocalContext.current.findHostActivity()
    val useSystemRuntime = !forceDemoRequest &&
        grantChecker is SystemPermissionGrantChecker &&
        activity != null
    val fallbackDemoChecker = remember {
        DemoPermissionGrantChecker(demoSession = PermissionDemoSession())
    }
    val effectiveGrantChecker = if (useSystemRuntime) {
        grantChecker
    } else if (grantChecker is DemoPermissionGrantChecker) {
        grantChecker
    } else {
        fallbackDemoChecker
    }
    val effectiveDemoSession = (effectiveGrantChecker as? DemoPermissionGrantChecker)?.demoSession
    val resultCallbackHolder = remember { PermissionResultCallbackHolder() }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        resultCallbackHolder.deliver(results)
    }
    val runtimeRequester = remember(permissionLauncher, resultCallbackHolder) {
        ComposeRuntimePermissionRequester(
            launcher = permissionLauncher,
            callbackHolder = resultCallbackHolder,
        )
    }
    return remember(
        scope,
        grantChecker,
        dialogController,
        snackbarDispatcher,
        demoSession,
        effectiveGrantChecker,
        runtimeRequester,
        useSystemRuntime,
        title,
        message,
        confirmText,
        dismissText,
        successMessage,
        deniedMessage,
    ) {
        PermissionInterceptUiImpl(
            scope = scope,
            grantChecker = effectiveGrantChecker,
            demoSession = effectiveDemoSession,
            runtimeRequester = if (useSystemRuntime) runtimeRequester else null,
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
 * 权限路由拦截器：识别 [RoutePermissionMarker]，未授权时弹出引导弹窗，成功后重定向至干净 path 并继续后续拦截器。
 */
class PermissionInterceptor(
    private val grantChecker: PermissionGrantChecker,
    private val permissionUi: PermissionInterceptUi,
    private val permissionDeniedMessage: String,
    override val priority: Int = RouterInterceptorPriorities.LOGIN + 50,
    override val showsLoading: Boolean = false,
) : PermissionRouteInterceptor {

    override suspend fun intercept(context: RouteInterceptContext): RouteInterceptResult {
        val parsed = RoutePermissionMarker.parse(context.targetRoute)
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
            return RouteInterceptResult.Abort(userMessage = null)
        }

        return redirectIfNeeded(parsed.cleanRoute, context.targetRoute)
    }

    private fun redirectIfNeeded(
        cleanRoute: String,
        currentRoute: String,
    ): RouteInterceptResult {
        return if (cleanRoute != currentRoute) {
            RouteInterceptResult.Redirect(cleanRoute)
        } else {
            RouteInterceptResult.Proceed
        }
    }
}

@Stable
private class PermissionResultCallbackHolder {
    private var onResult: ((Map<String, Boolean>) -> Unit)? = null

    fun await(onReady: (Map<String, Boolean>) -> Unit) {
        onResult = onReady
    }

    fun deliver(results: Map<String, Boolean>) {
        onResult?.invoke(results)
        onResult = null
    }

    fun clear() {
        onResult = null
    }
}

private class ComposeRuntimePermissionRequester(
    private val launcher: ActivityResultLauncher<Array<String>>,
    private val callbackHolder: PermissionResultCallbackHolder,
) {
    suspend fun request(permissions: List<String>): Boolean {
        if (permissions.isEmpty()) {
            return true
        }
        return suspendCancellableCoroutine { continuation ->
            callbackHolder.await { results ->
                if (continuation.isActive) {
                    continuation.resume(results.values.all { granted -> granted })
                }
            }
            continuation.invokeOnCancellation {
                callbackHolder.clear()
            }
            launcher.launch(permissions.toTypedArray())
        }
    }
}

private class PermissionInterceptUiImpl(
    private val scope: CoroutineScope,
    private val grantChecker: PermissionGrantChecker,
    private val demoSession: PermissionDemoSession?,
    private val runtimeRequester: ComposeRuntimePermissionRequester?,
    private val dialogController: DialogController?,
    private val snackbarDispatcher: SnackbarDispatcher?,
    private val title: String,
    private val message: String,
    private val confirmText: String,
    private val dismissText: String,
    private val successMessage: String,
    private val deniedMessage: String,
) : PermissionInterceptUi {

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
                    controller.dismissAll()
                    val granted = resolvePermissionGrant(
                        permissions = permissions,
                        permissionGroup = permissionGroup,
                    )
                    if (granted) {
                        showOutcomeSnackbar(
                            message = successMessage,
                            type = SnackbarType.Success,
                        )
                    } else {
                        showOutcomeSnackbar(
                            message = deniedMessage,
                            type = SnackbarType.Error,
                        )
                    }
                    if (continuation.isActive) {
                        continuation.resume(granted)
                    }
                }
            },
            onDismiss = {
                controller.dismissAll()
                showOutcomeSnackbar(
                    message = deniedMessage,
                    type = SnackbarType.Error,
                )
                if (continuation.isActive) {
                    continuation.resume(false)
                }
            },
        )
    }

    private suspend fun resolvePermissionGrant(
        permissions: List<String>,
        permissionGroup: String,
    ): Boolean {
        val requester = runtimeRequester
        if (requester != null) {
            return requester.request(permissions)
        }
        return performMockGrant(permissionGroup)
    }

    private suspend fun performMockGrant(permissionGroup: String): Boolean {
        delay(MOCK_PERMISSION_DELAY_MS)
        demoSession?.markGroupGranted(permissionGroup)
        return true
    }

    private fun showOutcomeSnackbar(
        message: String,
        type: SnackbarType,
    ) {
        val dispatcher = snackbarDispatcher ?: return
        showSnackbar(
            dispatcher = dispatcher,
            message = message,
            type = type,
        )
    }

    private companion object {
        const val MOCK_PERMISSION_DELAY_MS: Long = 300L
    }
}

private tailrec fun Context.findHostActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findHostActivity()
        else -> null
    }
}
