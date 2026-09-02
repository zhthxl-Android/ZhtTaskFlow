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
import com.example.zhttaskflow.nav.deeplink.TaskFlowDeepLinkNavigation
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
 * [TaskFlowDeepLinkNavigation.prepareNavigationRoute] 在 `navigate` 前对 `target` 施加与 RouteHost 相同的登录 / 权限标记，
 * 再经深链拦截链：深链 `200` → 权限 `150` → 登录 `100`。
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

    private fun enqueueDeepLinkFromIntent(intent: Intent?) {
        val uri = TaskFlowDeepLinkNavigation.extractDeepLinkUri(intent) ?: return
        pendingDeepLinkUri.value = uri
    }
}

@Composable
private fun MainActivityDeepLinkEffect(
    navigator: TaskFlowNavigator,
    pendingUri: String?,
    onDeepLinkConsumed: () -> Unit,
) {
    LaunchedEffect(pendingUri) {
        val uri = pendingUri ?: return@LaunchedEffect
        navigator.navigate(TaskFlowDeepLinkNavigation.prepareNavigationRoute(uri))
        onDeepLinkConsumed()
    }
}
