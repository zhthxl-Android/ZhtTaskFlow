package com.example.zhttaskflow

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.zhttaskflow.navigation.AppMainShell
import com.example.zhttaskflow.navigation.registerAppRoutes
import com.example.zhttaskflow.nav.interceptor.TaskFlowRouteAuthMarker
import com.example.zhttaskflow.nav.interceptor.TaskFlowRouteDeepLinkMarker
import com.example.zhttaskflow.nav.interceptor.TaskFlowRoutePermissionMarker
import com.example.zhttaskflow.nav.rememberTaskFlowNavigator
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowHomeNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistryImpl
import com.example.zhttaskflow.nav.theme.TaskFlowTheme

/**
 * 壳 Activity：模块路由装配、[AppMainShell] 宿主，以及外部深链 [Intent] 统一入口。
 *
 * ## 标准深链格式（运营 / H5 / 推送可直接复用）
 *
 * 约定由 [com.example.zhttaskflow.nav.interceptor.TaskFlowDeepLinkRouteMapperImpl] 解析：
 *
 * - **Scheme**：`taskflow`
 * - **Host**：`nav`
 * - **Path**：`/route`
 * - **Query**：`target` = URL 编码后的**内部已注册路由 path**（非页面 URL）
 *
 * 示例（任务详情，taskId = demo-1）：
 * ```
 * taskflow://nav/route?target=feature_task%2Fdetail%2Fdemo-1
 * ```
 *
 * ## 门禁与内链一致
 *
 * [prepareDeepLinkNavigationRoute] 在 `navigate` 前对 `target` 施加与 RouteHost 相同的登录 / 权限标记（如任务详情），
 * 再经 [TaskFlowRouteDeepLinkMarker.wrap] 进入拦截链：深链 `200` → 权限 `150` → 登录 `100`。
 * 解析失败、未映射或拦截中止时由链上 UI 桥统一 Snackbar（Error），与内部跳转一致。
 *
 * ## 接入说明
 *
 * 1. 外部通过 `ACTION_VIEW` 拉起本 Activity（Manifest 已声明 `taskflow` scheme）。
 * 2. [onCreate] / [onNewIntent] 提取 `Intent.data`，写入待处理队列，**不**在 Activity 内直接 `NavController.navigate`。
 * 3. [MainActivityDeepLinkEffect] 在 [AppMainShell] 组合完成后调用 [TaskFlowNavigator.navigate]。
 * 4. 桌面图标 `MAIN` / `LAUNCHER` 启动无 `data`，保持原首页逻辑；应用内路由不受影响。
 *
 * ## 本地验证（adb）
 *
 * ```
 * adb shell am start -a android.intent.action.VIEW -d "taskflow://nav/route?target=feature_task%2Fdetail%2Fdemo-1" com.example.zhttaskflow
 * ```
 */
class MainActivity : ComponentActivity() {

    /** 待消费的外部深链 URI（Compose 侧在 NavHost 装配拦截链后统一 navigate）。 */
    private val pendingDeepLinkUri = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enqueueDeepLinkFromIntent(intent)
        enableEdgeToEdge()
        setContent {
            TaskFlowTheme {
                val navigator = rememberTaskFlowNavigator()
                val routeRegistry = remember(navigator) {
                    TaskFlowRouteRegistryImpl().also { registry ->
                        registry.registerAppRoutes(
                            navigator = navigator,
                            onHomeBackPress = { moveTaskToBack(true) },
                        )
                    }
                }
                AppMainShell(
                    registry = routeRegistry,
                    startDestination = TaskFlowHomeNavRoutes.HOME_ROUTE,
                    navigator = navigator,
                )
                MainActivityDeepLinkEffect(
                    navigator = navigator,
                    pendingUri = pendingDeepLinkUri.value,
                    onDeepLinkConsumed = { pendingDeepLinkUri.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        enqueueDeepLinkFromIntent(intent)
    }

    /**
     * 从启动 / 唤起 Intent 解析深链；普通桌面启动返回 `null`，不改变默认首页。
     */
    private fun enqueueDeepLinkFromIntent(intent: Intent?) {
        val uri = extractDeepLinkUri(intent) ?: return
        pendingDeepLinkUri.value = uri
    }

    private fun extractDeepLinkUri(intent: Intent?): String? {
        if (intent == null) {
            return null
        }
        if (intent.action != Intent.ACTION_VIEW) {
            return null
        }
        return intent.data?.toString()?.takeIf { it.isNotBlank() }
    }
}

/**
 * 在 [AppMainShell] 子树完成组合（拦截链已 [androidx.compose.runtime.SideEffect] 注入）后分发深链，
 * 保证与内部跳转共用 [TaskFlowNavigator.navigate] 与拦截链校验。
 */
@Composable
private fun MainActivityDeepLinkEffect(
    navigator: TaskFlowNavigator,
    pendingUri: String?,
    onDeepLinkConsumed: () -> Unit,
) {
    LaunchedEffect(pendingUri) {
        val uri = pendingUri ?: return@LaunchedEffect
        val route = prepareDeepLinkNavigationRoute(uri)
        navigator.navigate(route)
        onDeepLinkConsumed()
    }
}

/**
 * 构造进入拦截链的导航 path：先对标准 `target` 叠加壳层门禁策略，再 [TaskFlowRouteDeepLinkMarker.wrap]。
 */
internal fun prepareDeepLinkNavigationRoute(externalUri: String): String {
    val enrichedUri = enrichExternalDeepLinkUri(externalUri.trim())
    return TaskFlowRouteDeepLinkMarker.wrap(enrichedUri)
}

/**
 * 对 `taskflow://nav/route?target=...` 的 target 施加与业务 RouteHost 一致的门禁 query，其它 URI 原样返回（由深链拦截器校验）。
 */
internal fun enrichExternalDeepLinkUri(externalUri: String): String {
    val uri = runCatching { Uri.parse(externalUri) }.getOrNull() ?: return externalUri
    val rawTarget = extractStandardDeepLinkTarget(uri) ?: return externalUri
    val gatedTarget = applyShellRouteGatePolicy(rawTarget)
    if (gatedTarget == rawTarget) {
        return externalUri
    }
    return buildStandardDeepLinkUri(gatedTarget)
}

private fun extractStandardDeepLinkTarget(uri: Uri): String? {
    if (uri.scheme != MainActivityDeepLinkConstants.SCHEME) {
        return null
    }
    if (uri.host != MainActivityDeepLinkConstants.HOST_NAV) {
        return null
    }
    if (uri.path != MainActivityDeepLinkConstants.PATH_ROUTE) {
        return null
    }
    return uri.getQueryParameter(MainActivityDeepLinkConstants.QUERY_TARGET)
        ?.let { target -> Uri.decode(target).takeIf { it.isNotBlank() } }
}

private fun buildStandardDeepLinkUri(encodedTargetPath: String): String {
    val builder = Uri.Builder()
        .scheme(MainActivityDeepLinkConstants.SCHEME)
        .authority(MainActivityDeepLinkConstants.HOST_NAV)
        .appendPath(MainActivityDeepLinkConstants.PATH_ROUTE.trimStart('/'))
        .appendQueryParameter(
            MainActivityDeepLinkConstants.QUERY_TARGET,
            encodedTargetPath,
        )
    return builder.build().toString()
}

/**
 * 壳层路由门禁表：与 Feature RouteHost 在 `navigate` 前的标记策略对齐（新增页面在此扩展）。
 */
private fun applyShellRouteGatePolicy(mappedRoute: String): String {
    val pathOnly = mappedRoute.substringBefore('?')
    if (!isTaskDetailNavigationPath(pathOnly)) {
        return mappedRoute
    }
    return TaskFlowRoutePermissionMarker.withStoragePermission(
        TaskFlowRouteAuthMarker.withNeedLogin(pathOnly),
    )
}

private fun isTaskDetailNavigationPath(pathWithoutQuery: String): Boolean {
    val expectedPrefix = "feature_task/detail/"
    if (!pathWithoutQuery.startsWith(expectedPrefix)) {
        return false
    }
    val taskId = pathWithoutQuery.removePrefix(expectedPrefix)
    return taskId.isNotBlank() && !taskId.contains('/')
}

private object MainActivityDeepLinkConstants {
    const val SCHEME: String = "taskflow"
    const val HOST_NAV: String = "nav"
    const val PATH_ROUTE: String = "/route"
    const val QUERY_TARGET: String = "target"
}
