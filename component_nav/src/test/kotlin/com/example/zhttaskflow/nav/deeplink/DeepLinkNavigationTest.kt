package com.example.zhttaskflow.nav.deeplink

import android.content.Intent
import android.net.Uri
import com.example.zhttaskflow.nav.interceptor.DeepLinkInterceptor
import com.example.zhttaskflow.nav.interceptor.DeepLinkRouteMapperImpl
import com.example.zhttaskflow.nav.interceptor.LoginInterceptUi
import com.example.zhttaskflow.nav.interceptor.LoginInterceptor
import com.example.zhttaskflow.nav.interceptor.LoginSession
import com.example.zhttaskflow.nav.interceptor.PermissionInterceptUi
import com.example.zhttaskflow.nav.interceptor.PermissionDemoSession
import com.example.zhttaskflow.nav.interceptor.PermissionGroups
import com.example.zhttaskflow.nav.interceptor.PermissionInterceptor
import com.example.zhttaskflow.nav.interceptor.RouteAuthMarker
import com.example.zhttaskflow.nav.interceptor.RouteDeepLinkMarker
import com.example.zhttaskflow.nav.interceptor.RouteGatePolicy
import com.example.zhttaskflow.nav.interceptor.RoutePermissionMarker
import com.example.zhttaskflow.nav.interceptor.DemoPermissionGrantChecker
import com.example.zhttaskflow.nav.interceptor.RouterInterceptorPriorities
import com.example.zhttaskflow.nav.route.ArticleNavRoutes
import com.example.zhttaskflow.nav.route.TaskNavRoutes
import com.example.zhttaskflow.nav.router.LoginRouteInterceptor
import com.example.zhttaskflow.nav.router.PermissionRouteInterceptor
import com.example.zhttaskflow.nav.router.RouteInterceptContext
import com.example.zhttaskflow.nav.router.RouteInterceptResult
import com.example.zhttaskflow.nav.router.RouteRequest
import com.example.zhttaskflow.nav.router.RouterChainOutcome
import com.example.zhttaskflow.nav.router.RouterInterceptorChain
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
class DeepLinkNavigationTest {

    private val parseErrorMessage = "deeplink-parse-failed"
    private val unmappedErrorMessage = "deeplink-unmapped"
    private val permissionDeniedMessage = "permission-denied"

    @Test
    fun extractDeepLinkUri_actionView_returnsData() {
        val uri = buildStandardDeepLink(TaskNavRoutes.detailPath("demo-1"))
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))

        assertEquals(uri, DeepLinkNavigation.extractDeepLinkUri(intent))
    }

    @Test
    fun extractDeepLinkUri_nonViewAction_returnsNull() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            data = Uri.parse(buildStandardDeepLink(TaskNavRoutes.TASK_LIST))
        }
        assertNull(DeepLinkNavigation.extractDeepLinkUri(intent))
    }

    @Test
    fun enrichExternalDeepLinkUri_injectsRouteGateMarkersIntoTarget() {
        val rawTarget = TaskNavRoutes.detailPath("adb-demo")
        val external = buildStandardDeepLink(rawTarget)
        val enriched = DeepLinkNavigation.enrichExternalDeepLinkUri(external)

        val target = Uri.parse(enriched).getQueryParameter("target")?.let(Uri::decode).orEmpty()
        assertEquals(RouteGatePolicy.enrichNavigationPath(rawTarget), target)
    }

    @Test
    fun prepareNavigationRoute_wrapsEnrichedUriForInterceptorChain() {
        val external = buildStandardDeepLink(TaskNavRoutes.TASK_LIST)
        val navigationRoute = DeepLinkNavigation.prepareNavigationRoute(external)

        assertTrue(navigationRoute.startsWith("@deeplink/"))
        val roundTrip = RouteDeepLinkMarker.parse(navigationRoute)
        assertEquals(DeepLinkNavigation.enrichExternalDeepLinkUri(external), roundTrip)
    }

    @Test
    fun applyShellRouteGatePolicy_matchesRouteGatePolicy() {
        val path = ArticleNavRoutes.detailPath("n1", "https://news.example/item")
        assertEquals(
            RouteGatePolicy.enrichNavigationPath(path),
            DeepLinkNavigation.applyShellRouteGatePolicy(path),
        )
    }

    @Test
    fun interceptorChain_validDeepLink_navigatesToCleanPathWhenGatesSatisfied() = runTest {
        val taskId = "demo-1"
        val external = buildStandardDeepLink(TaskNavRoutes.detailPath(taskId))
        val chain = defaultAppLikeChain(
            loginSession = LoginSession().apply { markLoggedIn() },
            permissionDemoSession = PermissionDemoSession().apply {
                markGroupGranted(PermissionGroups.STORAGE)
            },
            loginUi = { true },
            permissionUi = { _, _ -> true },
        )

        val outcome = chain.intercept(
            RouteRequest(
                targetRoute = DeepLinkNavigation.prepareNavigationRoute(external),
            ),
        )

        assertTrue(outcome is RouterChainOutcome.Navigate)
        assertEquals(TaskNavRoutes.detailPath(taskId), (outcome as RouterChainOutcome.Navigate).route)
    }

    @Test
    fun interceptorChain_blankDeepLinkMarker_proceedsWithoutNavigationFailure() = runTest {
        val chain = defaultAppLikeChain()
        val outcome = chain.intercept(RouteRequest(targetRoute = "@deeplink/"))

        assertTrue(outcome is RouterChainOutcome.Navigate)
        assertEquals("@deeplink/", (outcome as RouterChainOutcome.Navigate).route)
    }

    @Test
    fun interceptorChain_deepLinkTaskDetail_blocksWhenPermissionDenied() = runTest {
        val external = buildStandardDeepLink(TaskNavRoutes.detailPath("perm-gate"))
        val chain = defaultAppLikeChain(
            loginSession = LoginSession().apply { markLoggedIn() },
            permissionDemoSession = PermissionDemoSession(),
            loginUi = { true },
            permissionUi = { _, _ -> false },
        )

        val outcome = chain.intercept(
            RouteRequest(
                targetRoute = DeepLinkNavigation.prepareNavigationRoute(external),
            ),
        )

        assertTrue(outcome is RouterChainOutcome.Cancelled)
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
            RouteRequest(
                targetRoute = DeepLinkNavigation.prepareNavigationRoute(external),
            ),
        )

        assertTrue(outcome is RouterChainOutcome.Cancelled)
        assertEquals(unmappedErrorMessage, (outcome as RouterChainOutcome.Cancelled).message)
    }

    @Test
    fun interceptorChain_deepLinkArticleDetail_preservesEncodedPathSegments() = runTest {
        val articleId = "article-42"
        val detailUrl = "https://example.com/item"
        val target = ArticleNavRoutes.detailPath(articleId, detailUrl)
        val external = buildStandardDeepLink(target)
        val chain = defaultAppLikeChain(
            loginSession = LoginSession().apply { markLoggedIn() },
            permissionDemoSession = PermissionDemoSession(),
            loginUi = { true },
            permissionUi = { _, _ -> true },
        )

        val outcome = chain.intercept(
            RouteRequest(
                targetRoute = DeepLinkNavigation.prepareNavigationRoute(external),
            ),
        )

        assertTrue(outcome is RouterChainOutcome.Navigate)
        assertEquals(
            "feature_article/detail/$articleId/$detailUrl",
            (outcome as RouterChainOutcome.Navigate).route,
        )
    }

    @Test
    fun interceptorChain_deepLinkTaskDetail_blocksWhenLoginCancelled() = runTest {
        val external = buildStandardDeepLink(TaskNavRoutes.detailPath("gate-task"))
        val chain = defaultAppLikeChain(
            loginSession = LoginSession(),
            permissionDemoSession = PermissionDemoSession().apply {
                markGroupGranted(PermissionGroups.STORAGE)
            },
            loginUi = { false },
            permissionUi = { _, _ -> true },
        )

        val outcome = chain.intercept(
            RouteRequest(
                targetRoute = DeepLinkNavigation.prepareNavigationRoute(external),
            ),
        )

        assertTrue(outcome is RouterChainOutcome.Cancelled)
    }

    @Test
    fun interceptorChain_marksRequireLoginAndPermissionBeforeStrippingQueries() = runTest {
        val gatedPath = RouteGatePolicy.enrichNavigationPath(
            TaskNavRoutes.detailPath("marker-check"),
        )
        assertTrue(RouteAuthMarker.parse(gatedPath).requiresLogin)
        assertEquals(
            PermissionGroups.STORAGE,
            RoutePermissionMarker.parse(gatedPath).permissionGroup,
        )
    }

    @Test
    fun interceptorChain_executionOrder_deepLinkRunsBeforeLoginAndPermission() = runTest {
        val order = mutableListOf<String>()
        val mapper = DeepLinkRouteMapperImpl.defaultBuilder().build()
        val chain = RouterInterceptorChain.build {
            add(
                DeepLinkInterceptor(
                    routeMapper = mapper,
                    parseErrorMessage = parseErrorMessage,
                    unmappedErrorMessage = unmappedErrorMessage,
                    priority = RouterInterceptorPriorities.DEEP_LINK,
                ),
            )
            add(
                object : PermissionRouteInterceptor {
                    override val priority: Int = RouterInterceptorPriorities.LOGIN + 50
                    override suspend fun intercept(context: RouteInterceptContext): RouteInterceptResult {
                        order.add("permission")
                        return RouteInterceptResult.Proceed
                    }
                },
            )
            add(
                object : LoginRouteInterceptor {
                    override val priority: Int = RouterInterceptorPriorities.LOGIN
                    override suspend fun intercept(context: RouteInterceptContext): RouteInterceptResult {
                        order.add("login")
                        return RouteInterceptResult.Proceed
                    }
                },
            )
        }

        chain.intercept(
            RouteRequest(
                targetRoute = DeepLinkNavigation.prepareNavigationRoute(
                    buildStandardDeepLink(TaskNavRoutes.TASK_LIST),
                ),
            ),
        )

        assertEquals(listOf("permission", "login"), order)
    }

    private fun defaultAppLikeChain(
        loginSession: LoginSession = LoginSession(),
        permissionDemoSession: PermissionDemoSession = PermissionDemoSession(),
        loginUi: suspend () -> Boolean = { false },
        permissionUi: suspend (List<String>, String) -> Boolean = { _, _ -> false },
    ): RouterInterceptorChain {
        val mapper = DeepLinkRouteMapperImpl.defaultBuilder().build()
        val grantChecker = DemoPermissionGrantChecker(permissionDemoSession)
        return RouterInterceptorChain.build {
            add(
                DeepLinkInterceptor(
                    routeMapper = mapper,
                    parseErrorMessage = parseErrorMessage,
                    unmappedErrorMessage = unmappedErrorMessage,
                ),
            )
            add(
                LoginInterceptor(
                    loginSession = loginSession,
                    loginUi = LoginInterceptUi { loginUi() },
                    priority = RouterInterceptorPriorities.LOGIN,
                ),
            )
            add(
                PermissionInterceptor(
                    grantChecker = grantChecker,
                    permissionUi = PermissionInterceptUi { permissions, group ->
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
