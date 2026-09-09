package com.example.zhttaskflow.nav.interceptor

import android.net.Uri
import com.example.zhttaskflow.nav.route.ArticleNavRoutes
import com.example.zhttaskflow.nav.route.LogNavRoutes
import com.example.zhttaskflow.nav.route.TaskNavRoutes
import com.example.zhttaskflow.nav.router.RouteInterceptContext
import com.example.zhttaskflow.nav.router.RouteInterceptResult
import com.example.zhttaskflow.nav.router.RouteRequest
import com.example.zhttaskflow.nav.router.RouterInterceptor
import com.example.zhttaskflow.nav.router.RouterInterceptorChain
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [RouteGatePolicy] 与默认拦截链优先级回归测试（对齐 `docs/TASKFLOW_ROUTE_GATES.md` 手动场景）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RouteGatePolicyTest {

    @Test
    fun enrichNavigationPath_taskDetail_addsLoginAndStoragePermission() {
        val clean = TaskNavRoutes.detailPath("demo-1")
        val enriched = RouteGatePolicy.enrichNavigationPath(clean)

        val auth = RouteAuthMarker.parse(enriched)
        val permission = RoutePermissionMarker.parse(enriched)

        assertTrue(auth.requiresLogin)
        assertEquals(clean, auth.cleanRoute)
        assertEquals(PermissionGroups.STORAGE, permission.permissionGroup)
        assertEquals("$clean?needLogin=true", permission.cleanRoute)
    }

    @Test
    fun enrichNavigationPath_articleDetail_addsLoginOnly() {
        val clean = ArticleNavRoutes.detailPath("article-1", "https://example.com/page")
        val enriched = RouteGatePolicy.enrichNavigationPath(clean)

        val auth = RouteAuthMarker.parse(enriched)
        val permission = RoutePermissionMarker.parse(enriched)

        assertTrue(auth.requiresLogin)
        assertEquals(clean, auth.cleanRoute)
        assertEquals(null, permission.permissionGroup)
    }

    @Test
    fun enrichNavigationPath_unmarkedRoutes_passThroughUnchanged() {
        val routes = listOf(
            LogNavRoutes.LOG_ROUTE,
            TaskNavRoutes.TASK_LIST,
            ArticleNavRoutes.ARTICLE_LIST,
        )
        routes.forEach { path ->
            assertEquals(path, RouteGatePolicy.enrichNavigationPath(path))
        }
        assertTrue(RouteGatePolicy.isLogNavigationPath(LogNavRoutes.LOG_ROUTE))
        assertTrue(RouteGatePolicy.isTaskListNavigationPath(TaskNavRoutes.TASK_LIST))
        assertTrue(RouteGatePolicy.isArticleListNavigationPath(ArticleNavRoutes.ARTICLE_LIST))
    }

    @Test
    fun enrichNavigationPath_rebuildsMarkersFromPathOnly_avoidsDuplicateQuery() {
        val clean = TaskNavRoutes.detailPath("demo-2")
        val first = RouteGatePolicy.enrichNavigationPath(clean)
        val second = RouteGatePolicy.enrichNavigationPath(first)

        assertEquals(first, second)
        assertEquals(1, first.split("needLogin=").size - 1)
        assertEquals(1, first.split("permissionGroup=").size - 1)
    }

    @Test
    fun isTaskDetailNavigationPath_rejectsInvalidPaths() {
        assertFalse(RouteGatePolicy.isTaskDetailNavigationPath(TaskNavRoutes.TASK_LIST))
        assertFalse(RouteGatePolicy.isTaskDetailNavigationPath("feature_task/detail/"))
        assertFalse(RouteGatePolicy.isTaskDetailNavigationPath("feature_task/detail/a/extra"))
    }

    @Test
    fun deepLinkTarget_inheritsSameGatePolicyAsInAppNavigation() {
        val target = TaskNavRoutes.detailPath("deeplink-task")
        val externalUri = buildDeepLinkUri(target)
        val enrichedUri = com.example.zhttaskflow.nav.deeplink.DeepLinkNavigation
            .enrichExternalDeepLinkUri(externalUri)

        val decodedTarget = Uri.parse(enrichedUri).getQueryParameter("target")?.let(Uri::decode).orEmpty()
        val inApp = RouteGatePolicy.enrichNavigationPath(target)

        assertEquals(inApp, decodedTarget)
    }

  /**
     * 默认链执行顺序：深链（200）→ 权限（150）→ 登录（100），与 [RouterInterceptorPriorities] 一致。
     * 用户可见流程仍为「先登录引导、再存储权限」，由拦截器 UI 与标记剥离顺序共同保证。
     */
    @Test
    fun routerInterceptorChain_priorityOrder_deepLinkThenPermissionThenLogin() = runTest {
        val executionOrder = mutableListOf<String>()
        val chain = RouterInterceptorChain.build {
            add(
                recordingInterceptor(
                    name = "login",
                    priority = RouterInterceptorPriorities.LOGIN,
                    executionOrder = executionOrder,
                ),
            )
            add(
                recordingInterceptor(
                    name = "permission",
                    priority = RouterInterceptorPriorities.LOGIN + 50,
                    executionOrder = executionOrder,
                ),
            )
            add(
                recordingInterceptor(
                    name = "deeplink",
                    priority = RouterInterceptorPriorities.DEEP_LINK,
                    executionOrder = executionOrder,
                ),
            )
        }

        val outcome = chain.intercept(RouteRequest(targetRoute = "feature_task/list"))
        assertTrue(outcome is com.example.zhttaskflow.nav.router.RouterChainOutcome.Navigate)
        assertEquals(listOf("deeplink", "permission", "login"), executionOrder)
    }

    private fun recordingInterceptor(
        name: String,
        priority: Int,
        executionOrder: MutableList<String>,
    ): RouterInterceptor {
        return object : RouterInterceptor {
            override val priority: Int = priority

            override suspend fun intercept(context: RouteInterceptContext): RouteInterceptResult {
                executionOrder.add(name)
                return RouteInterceptResult.Proceed
            }
        }
    }

    private fun buildDeepLinkUri(targetPath: String): String {
        return Uri.Builder()
            .scheme("taskflow")
            .authority("nav")
            .appendPath("route")
            .appendQueryParameter("target", targetPath)
            .build()
            .toString()
    }
}
