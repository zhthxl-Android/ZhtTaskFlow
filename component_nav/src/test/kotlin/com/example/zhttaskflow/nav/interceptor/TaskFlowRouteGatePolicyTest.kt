package com.example.zhttaskflow.nav.interceptor

import android.net.Uri
import com.example.zhttaskflow.nav.route.TaskFlowArticleNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowLogNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowTaskNavRoutes
import com.example.zhttaskflow.nav.router.TaskFlowRouteInterceptContext
import com.example.zhttaskflow.nav.router.TaskFlowRouteInterceptResult
import com.example.zhttaskflow.nav.router.TaskFlowRouteRequest
import com.example.zhttaskflow.nav.router.TaskFlowRouterInterceptor
import com.example.zhttaskflow.nav.router.TaskFlowRouterInterceptorChain
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [TaskFlowRouteGatePolicy] 与默认拦截链优先级回归测试（对齐 `docs/TASKFLOW_ROUTE_GATES.md` 手动场景）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TaskFlowRouteGatePolicyTest {

    @Test
    fun enrichNavigationPath_taskDetail_addsLoginAndStoragePermission() {
        val clean = TaskFlowTaskNavRoutes.detailPath("demo-1")
        val enriched = TaskFlowRouteGatePolicy.enrichNavigationPath(clean)

        val auth = TaskFlowRouteAuthMarker.parse(enriched)
        val permission = TaskFlowRoutePermissionMarker.parse(enriched)

        assertTrue(auth.requiresLogin)
        assertEquals(clean, auth.cleanRoute)
        assertEquals(TaskFlowPermissionGroups.STORAGE, permission.permissionGroup)
        assertEquals("$clean?needLogin=true", permission.cleanRoute)
    }

    @Test
    fun enrichNavigationPath_articleDetail_addsLoginOnly() {
        val clean = TaskFlowArticleNavRoutes.detailPath("article-1", "https://example.com/page")
        val enriched = TaskFlowRouteGatePolicy.enrichNavigationPath(clean)

        val auth = TaskFlowRouteAuthMarker.parse(enriched)
        val permission = TaskFlowRoutePermissionMarker.parse(enriched)

        assertTrue(auth.requiresLogin)
        assertEquals(clean, auth.cleanRoute)
        assertEquals(null, permission.permissionGroup)
    }

    @Test
    fun enrichNavigationPath_unmarkedRoutes_passThroughUnchanged() {
        val routes = listOf(
            TaskFlowLogNavRoutes.LOG_ROUTE,
            TaskFlowTaskNavRoutes.TASK_LIST,
            TaskFlowArticleNavRoutes.ARTICLE_LIST,
        )
        routes.forEach { path ->
            assertEquals(path, TaskFlowRouteGatePolicy.enrichNavigationPath(path))
        }
        assertTrue(TaskFlowRouteGatePolicy.isLogNavigationPath(TaskFlowLogNavRoutes.LOG_ROUTE))
        assertTrue(TaskFlowRouteGatePolicy.isTaskListNavigationPath(TaskFlowTaskNavRoutes.TASK_LIST))
        assertTrue(TaskFlowRouteGatePolicy.isArticleListNavigationPath(TaskFlowArticleNavRoutes.ARTICLE_LIST))
    }

    @Test
    fun enrichNavigationPath_rebuildsMarkersFromPathOnly_avoidsDuplicateQuery() {
        val clean = TaskFlowTaskNavRoutes.detailPath("demo-2")
        val first = TaskFlowRouteGatePolicy.enrichNavigationPath(clean)
        val second = TaskFlowRouteGatePolicy.enrichNavigationPath(first)

        assertEquals(first, second)
        assertEquals(1, first.split("needLogin=").size - 1)
        assertEquals(1, first.split("permissionGroup=").size - 1)
    }

    @Test
    fun isTaskDetailNavigationPath_rejectsInvalidPaths() {
        assertFalse(TaskFlowRouteGatePolicy.isTaskDetailNavigationPath(TaskFlowTaskNavRoutes.TASK_LIST))
        assertFalse(TaskFlowRouteGatePolicy.isTaskDetailNavigationPath("feature_task/detail/"))
        assertFalse(TaskFlowRouteGatePolicy.isTaskDetailNavigationPath("feature_task/detail/a/extra"))
    }

    @Test
    fun deepLinkTarget_inheritsSameGatePolicyAsInAppNavigation() {
        val target = TaskFlowTaskNavRoutes.detailPath("deeplink-task")
        val externalUri = buildDeepLinkUri(target)
        val enrichedUri = com.example.zhttaskflow.nav.deeplink.TaskFlowDeepLinkNavigation
            .enrichExternalDeepLinkUri(externalUri)

        val decodedTarget = Uri.parse(enrichedUri).getQueryParameter("target")?.let(Uri::decode).orEmpty()
        val inApp = TaskFlowRouteGatePolicy.enrichNavigationPath(target)

        assertEquals(inApp, decodedTarget)
    }

  /**
     * 默认链执行顺序：深链（200）→ 权限（150）→ 登录（100），与 [TaskFlowRouterInterceptorPriorities] 一致。
     * 用户可见流程仍为「先登录引导、再存储权限」，由拦截器 UI 与标记剥离顺序共同保证。
     */
    @Test
    fun routerInterceptorChain_priorityOrder_deepLinkThenPermissionThenLogin() = runTest {
        val executionOrder = mutableListOf<String>()
        val chain = TaskFlowRouterInterceptorChain.build {
            add(
                recordingInterceptor(
                    name = "login",
                    priority = TaskFlowRouterInterceptorPriorities.LOGIN,
                    executionOrder = executionOrder,
                ),
            )
            add(
                recordingInterceptor(
                    name = "permission",
                    priority = TaskFlowRouterInterceptorPriorities.LOGIN + 50,
                    executionOrder = executionOrder,
                ),
            )
            add(
                recordingInterceptor(
                    name = "deeplink",
                    priority = TaskFlowRouterInterceptorPriorities.DEEP_LINK,
                    executionOrder = executionOrder,
                ),
            )
        }

        val outcome = chain.intercept(TaskFlowRouteRequest(targetRoute = "feature_task/list"))
        assertTrue(outcome is com.example.zhttaskflow.nav.router.TaskFlowRouterChainOutcome.Navigate)
        assertEquals(listOf("deeplink", "permission", "login"), executionOrder)
    }

    private fun recordingInterceptor(
        name: String,
        priority: Int,
        executionOrder: MutableList<String>,
    ): TaskFlowRouterInterceptor {
        return object : TaskFlowRouterInterceptor {
            override val priority: Int = priority

            override suspend fun intercept(context: TaskFlowRouteInterceptContext): TaskFlowRouteInterceptResult {
                executionOrder.add(name)
                return TaskFlowRouteInterceptResult.Proceed
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
