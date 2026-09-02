package com.example.zhttaskflow.nav.deeplink

import android.content.Intent
import android.net.Uri
import com.example.zhttaskflow.nav.interceptor.TaskFlowDeepLinkInterceptor
import com.example.zhttaskflow.nav.interceptor.TaskFlowDeepLinkRouteMapperImpl
import com.example.zhttaskflow.nav.interceptor.TaskFlowLoginInterceptUi
import com.example.zhttaskflow.nav.interceptor.TaskFlowLoginInterceptor
import com.example.zhttaskflow.nav.interceptor.TaskFlowLoginSession
import com.example.zhttaskflow.nav.interceptor.TaskFlowPermissionInterceptUi
import com.example.zhttaskflow.nav.interceptor.TaskFlowPermissionDemoSession
import com.example.zhttaskflow.nav.interceptor.TaskFlowPermissionGroups
import com.example.zhttaskflow.nav.interceptor.TaskFlowPermissionInterceptor
import com.example.zhttaskflow.nav.interceptor.TaskFlowRouteAuthMarker
import com.example.zhttaskflow.nav.interceptor.TaskFlowRouteDeepLinkMarker
import com.example.zhttaskflow.nav.interceptor.TaskFlowRouteGatePolicy
import com.example.zhttaskflow.nav.interceptor.TaskFlowRoutePermissionMarker
import com.example.zhttaskflow.nav.interceptor.TaskFlowDemoPermissionGrantChecker
import com.example.zhttaskflow.nav.interceptor.TaskFlowRouterInterceptorPriorities
import com.example.zhttaskflow.nav.route.TaskFlowArticleNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowTaskNavRoutes
import com.example.zhttaskflow.nav.router.TaskFlowLoginRouteInterceptor
import com.example.zhttaskflow.nav.router.TaskFlowPermissionRouteInterceptor
import com.example.zhttaskflow.nav.router.TaskFlowRouteInterceptContext
import com.example.zhttaskflow.nav.router.TaskFlowRouteInterceptResult
import com.example.zhttaskflow.nav.router.TaskFlowRouteRequest
import com.example.zhttaskflow.nav.router.TaskFlowRouterChainOutcome
import com.example.zhttaskflow.nav.router.TaskFlowRouterInterceptorChain
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 深链解析、参数注入与拦截链集成回归（对齐 `docs/TASKFLOW_ROUTE_GATES.md` 手动验证表）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TaskFlowDeepLinkNavigationTest {

    private val parseErrorMessage = "deeplink-parse-failed"
    private val unmappedErrorMessage = "deeplink-unmapped"
    private val permissionDeniedMessage = "permission-denied"

    @Test
    fun extractDeepLinkUri_actionView_returnsData() {
        val uri = buildStandardDeepLink(TaskFlowTaskNavRoutes.detailPath("demo-1"))
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))

        assertEquals(uri, TaskFlowDeepLinkNavigation.extractDeepLinkUri(intent))
    }

    @Test
    fun extractDeepLinkUri_nonViewAction_returnsNull() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            data = Uri.parse(buildStandardDeepLink(TaskFlowTaskNavRoutes.TASK_LIST))
        }
        assertNull(TaskFlowDeepLinkNavigation.extractDeepLinkUri(intent))
    }

    @Test
    fun enrichExternalDeepLinkUri_injectsRouteGateMarkersIntoTarget() {
        val rawTarget = TaskFlowTaskNavRoutes.detailPath("adb-demo")
        val external = buildStandardDeepLink(rawTarget)
        val enriched = TaskFlowDeepLinkNavigation.enrichExternalDeepLinkUri(external)

        val target = Uri.parse(enriched).getQueryParameter("target")?.let(Uri::decode).orEmpty()
        assertEquals(TaskFlowRouteGatePolicy.enrichNavigationPath(rawTarget), target)
    }

    @Test
    fun prepareNavigationRoute_wrapsEnrichedUriForInterceptorChain() {
        val external = buildStandardDeepLink(TaskFlowTaskNavRoutes.TASK_LIST)
        val navigationRoute = TaskFlowDeepLinkNavigation.prepareNavigationRoute(external)

        assertTrue(navigationRoute.startsWith("@deeplink/"))
        val roundTrip = TaskFlowRouteDeepLinkMarker.parse(navigationRoute)
        assertEquals(TaskFlowDeepLinkNavigation.enrichExternalDeepLinkUri(external), roundTrip)
    }

    @Test
    fun applyShellRouteGatePolicy_matchesRouteGatePolicy() {
        val path = TaskFlowArticleNavRoutes.detailPath("n1", "https://news.example/item")
        assertEquals(
            TaskFlowRouteGatePolicy.enrichNavigationPath(path),
            TaskFlowDeepLinkNavigation.applyShellRouteGatePolicy(path),
        )
    }

    @Test
    fun interceptorChain_validDeepLink_navigatesToCleanPathWhenGatesSatisfied() = runTest {
        val taskId = "demo-1"
        val external = buildStandardDeepLink(TaskFlowTaskNavRoutes.detailPath(taskId))
        val chain = defaultAppLikeChain(
            loginSession = TaskFlowLoginSession().apply { markLoggedIn() },
            permissionDemoSession = TaskFlowPermissionDemoSession().apply {
                markGroupGranted(TaskFlowPermissionGroups.STORAGE)
            },
            loginUi = { true },
            permissionUi = { _, _ -> true },
        )

        val outcome = chain.intercept(
            TaskFlowRouteRequest(
                targetRoute = TaskFlowDeepLinkNavigation.prepareNavigationRoute(external),
            ),
        )

        assertTrue(outcome is TaskFlowRouterChainOutcome.Navigate)
        assertEquals(TaskFlowTaskNavRoutes.detailPath(taskId), (outcome as TaskFlowRouterChainOutcome.Navigate).route)
    }

    @Test
    fun interceptorChain_blankDeepLinkMarker_proceedsWithoutNavigationFailure() = runTest {
        val chain = defaultAppLikeChain()
        val outcome = chain.intercept(TaskFlowRouteRequest(targetRoute = "@deeplink/"))

        assertTrue(outcome is TaskFlowRouterChainOutcome.Navigate)
        assertEquals("@deeplink/", (outcome as TaskFlowRouterChainOutcome.Navigate).route)
    }

    @Test
    fun interceptorChain_deepLinkTaskDetail_blocksWhenPermissionDenied() = runTest {
        val external = buildStandardDeepLink(TaskFlowTaskNavRoutes.detailPath("perm-gate"))
        val chain = defaultAppLikeChain(
            loginSession = TaskFlowLoginSession().apply { markLoggedIn() },
            permissionDemoSession = TaskFlowPermissionDemoSession(),
            loginUi = { true },
            permissionUi = { _, _ -> false },
        )

        val outcome = chain.intercept(
            TaskFlowRouteRequest(
                targetRoute = TaskFlowDeepLinkNavigation.prepareNavigationRoute(external),
            ),
        )

        assertTrue(outcome is TaskFlowRouterChainOutcome.Cancelled)
    }

    @Test
    fun interceptorChain_unmappedDeepLink_showsUnmappedError() = runTest {
        val chain = defaultAppLikeChain()
        val external = Uri.Builder()
            .scheme("taskflow")
            .authority("unknown-host")
            .appendPath("route")
            .appendQueryParameter("target", "feature_task/list")
            .build()
            .toString()

        val outcome = chain.intercept(
            TaskFlowRouteRequest(
                targetRoute = TaskFlowDeepLinkNavigation.prepareNavigationRoute(external),
            ),
        )

        assertTrue(outcome is TaskFlowRouterChainOutcome.Cancelled)
        assertEquals(unmappedErrorMessage, (outcome as TaskFlowRouterChainOutcome.Cancelled).message)
    }

    @Test
    fun interceptorChain_deepLinkArticleDetail_preservesEncodedPathSegments() = runTest {
        val articleId = "article-42"
        val detailUrl = "https://example.com/item"
        val target = TaskFlowArticleNavRoutes.detailPath(articleId, detailUrl)
        val external = buildStandardDeepLink(target)
        val chain = defaultAppLikeChain(
            loginSession = TaskFlowLoginSession().apply { markLoggedIn() },
            permissionDemoSession = TaskFlowPermissionDemoSession(),
            loginUi = { true },
            permissionUi = { _, _ -> true },
        )

        val outcome = chain.intercept(
            TaskFlowRouteRequest(
                targetRoute = TaskFlowDeepLinkNavigation.prepareNavigationRoute(external),
            ),
        )

        assertTrue(outcome is TaskFlowRouterChainOutcome.Navigate)
        assertEquals(
            "feature_article/detail/$articleId/$detailUrl",
            (outcome as TaskFlowRouterChainOutcome.Navigate).route,
        )
    }

    @Test
    fun interceptorChain_deepLinkTaskDetail_blocksWhenLoginCancelled() = runTest {
        val external = buildStandardDeepLink(TaskFlowTaskNavRoutes.detailPath("gate-task"))
        val chain = defaultAppLikeChain(
            loginSession = TaskFlowLoginSession(),
            permissionDemoSession = TaskFlowPermissionDemoSession().apply {
                markGroupGranted(TaskFlowPermissionGroups.STORAGE)
            },
            loginUi = { false },
            permissionUi = { _, _ -> true },
        )

        val outcome = chain.intercept(
            TaskFlowRouteRequest(
                targetRoute = TaskFlowDeepLinkNavigation.prepareNavigationRoute(external),
            ),
        )

        assertTrue(outcome is TaskFlowRouterChainOutcome.Cancelled)
    }

    @Test
    fun interceptorChain_marksRequireLoginAndPermissionBeforeStrippingQueries() = runTest {
        val gatedPath = TaskFlowRouteGatePolicy.enrichNavigationPath(
            TaskFlowTaskNavRoutes.detailPath("marker-check"),
        )
        assertTrue(TaskFlowRouteAuthMarker.parse(gatedPath).requiresLogin)
        assertEquals(
            TaskFlowPermissionGroups.STORAGE,
            TaskFlowRoutePermissionMarker.parse(gatedPath).permissionGroup,
        )
    }

    @Test
    fun interceptorChain_executionOrder_deepLinkRunsBeforeLoginAndPermission() = runTest {
        val order = mutableListOf<String>()
        val mapper = TaskFlowDeepLinkRouteMapperImpl.defaultBuilder().build()
        val chain = TaskFlowRouterInterceptorChain.build {
            add(
                TaskFlowDeepLinkInterceptor(
                    routeMapper = mapper,
                    parseErrorMessage = parseErrorMessage,
                    unmappedErrorMessage = unmappedErrorMessage,
                    priority = TaskFlowRouterInterceptorPriorities.DEEP_LINK,
                ),
            )
            add(
                object : TaskFlowPermissionRouteInterceptor {
                    override val priority: Int = TaskFlowRouterInterceptorPriorities.LOGIN + 50
                    override suspend fun intercept(context: TaskFlowRouteInterceptContext): TaskFlowRouteInterceptResult {
                        order.add("permission")
                        return TaskFlowRouteInterceptResult.Proceed
                    }
                },
            )
            add(
                object : TaskFlowLoginRouteInterceptor {
                    override val priority: Int = TaskFlowRouterInterceptorPriorities.LOGIN
                    override suspend fun intercept(context: TaskFlowRouteInterceptContext): TaskFlowRouteInterceptResult {
                        order.add("login")
                        return TaskFlowRouteInterceptResult.Proceed
                    }
                },
            )
        }

        chain.intercept(
            TaskFlowRouteRequest(
                targetRoute = TaskFlowDeepLinkNavigation.prepareNavigationRoute(
                    buildStandardDeepLink(TaskFlowTaskNavRoutes.TASK_LIST),
                ),
            ),
        )

        assertEquals(listOf("permission", "login"), order)
    }

    private fun defaultAppLikeChain(
        loginSession: TaskFlowLoginSession = TaskFlowLoginSession(),
        permissionDemoSession: TaskFlowPermissionDemoSession = TaskFlowPermissionDemoSession(),
        loginUi: suspend () -> Boolean = { false },
        permissionUi: suspend (List<String>, String) -> Boolean = { _, _ -> false },
    ): TaskFlowRouterInterceptorChain {
        val mapper = TaskFlowDeepLinkRouteMapperImpl.defaultBuilder().build()
        val grantChecker = TaskFlowDemoPermissionGrantChecker(permissionDemoSession)
        return TaskFlowRouterInterceptorChain.build {
            add(
                TaskFlowDeepLinkInterceptor(
                    routeMapper = mapper,
                    parseErrorMessage = parseErrorMessage,
                    unmappedErrorMessage = unmappedErrorMessage,
                ),
            )
            add(
                TaskFlowLoginInterceptor(
                    loginSession = loginSession,
                    loginUi = TaskFlowLoginInterceptUi { loginUi() },
                    priority = TaskFlowRouterInterceptorPriorities.LOGIN,
                ),
            )
            add(
                TaskFlowPermissionInterceptor(
                    grantChecker = grantChecker,
                    permissionUi = TaskFlowPermissionInterceptUi { permissions, group ->
                        permissionUi(permissions, group)
                    },
                    permissionDeniedMessage = permissionDeniedMessage,
                ),
            )
        }
    }

    private fun buildStandardDeepLink(targetPath: String): String {
        return Uri.Builder()
            .scheme("taskflow")
            .authority("nav")
            .appendPath("route")
            .appendQueryParameter("target", targetPath)
            .build()
            .toString()
    }
}
