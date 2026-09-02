package com.example.zhttaskflow

import android.content.Intent
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
import com.example.zhttaskflow.nav.interceptor.TaskFlowRouteDeepLinkMarker
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
 * 示例（文章列表 Tab 对应 path，按工程注册表填写）：
 * ```
 * taskflow://nav/route?target=feature_article%2Flist
 * ```
 *
 * ## 接入说明
 *
 * 1. 外部通过 `ACTION_VIEW` 拉起本 Activity（Manifest 已声明 `taskflow` scheme）。
 * 2. [onCreate] / [onNewIntent] 提取 `Intent.data`，写入待处理队列，**不**在 Activity 内直接 `NavController.navigate`。
 * 3. [MainActivityDeepLinkEffect] 在 [AppMainShell]（含 [com.example.zhttaskflow.nav.TaskFlowNavHost]）完成组合后，
 *    调用 [com.example.zhttaskflow.nav.TaskFlowNavigator.navigate] + [TaskFlowRouteDeepLinkMarker.wrap]，
 *    走默认拦截链（深链 `200` → 权限 `150` → 登录 `100`）；`target` 与 RouteHost 标记一致时可自动继承登录 / 权限门禁；失败由全局 Snackbar 提示。
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
 * 保证与内部跳转共用二级页转场与拦截校验。
 */
@Composable
private fun MainActivityDeepLinkEffect(
    navigator: TaskFlowNavigator,
    pendingUri: String?,
    onDeepLinkConsumed: () -> Unit,
) {
    LaunchedEffect(pendingUri) {
        val uri = pendingUri ?: return@LaunchedEffect
        navigator.navigate(TaskFlowRouteDeepLinkMarker.wrap(uri))
        onDeepLinkConsumed()
    }
}
