package com.example.zhttaskflow.nav

/**
 * 导航与路由旧 `TaskFlow*` 名的 `@Deprecated` 过渡层。
 *
 * **下个版本可统一移除**；新代码使用 `AppNavHost`、`AppNavigator`、`NavRoutes` 等短名。
 */
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.zhttaskflow.nav.interceptor.rememberAppRouterInterceptorChain
import com.example.zhttaskflow.nav.interceptor.rememberDeepLinkRouteMapper
import com.example.zhttaskflow.nav.interceptor.rememberLoginInterceptUi
import com.example.zhttaskflow.nav.interceptor.rememberLoginSession
import com.example.zhttaskflow.nav.interceptor.rememberPermissionGrantChecker
import com.example.zhttaskflow.nav.interceptor.rememberPermissionInterceptUi
import com.example.zhttaskflow.nav.route.ArticleNavRoutes
import com.example.zhttaskflow.nav.route.LogNavRoutes
import com.example.zhttaskflow.nav.route.NavRoutes
import com.example.zhttaskflow.nav.route.Route
import com.example.zhttaskflow.nav.route.RouteEntry
import com.example.zhttaskflow.nav.route.RouteRegistry
import com.example.zhttaskflow.nav.route.TaskNavRoutes
import com.example.zhttaskflow.nav.router.RouterInterceptor
import com.example.zhttaskflow.nav.router.RouterInterceptorChain
import com.example.zhttaskflow.nav.router.RouteInterceptResult
import com.example.zhttaskflow.nav.router.RouteRequest
import com.example.zhttaskflow.nav.interceptor.DeepLinkInterceptor
import com.example.zhttaskflow.nav.interceptor.LoginInterceptor
import com.example.zhttaskflow.nav.interceptor.PermissionInterceptor
import com.example.zhttaskflow.nav.deeplink.DeepLinkNavigation
import com.example.zhttaskflow.nav.interceptor.RouteGatePolicy
import com.example.zhttaskflow.nav.standalone.FeatureDebugShell
import com.example.zhttaskflow.nav.standalone.prepareFeatureDebug
import androidx.navigation.compose.rememberNavController
import com.example.zhttaskflow.nav.transition.NavTransitionRegistry
import com.example.zhttaskflow.nav.router.rememberRouterInterceptUiBridge

private const val DEPRECATION_MESSAGE = "将在下个版本移除，请使用新名称（见 ReplaceWith）"

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("RouteRegistry"))
typealias TaskFlowRouteRegistry = RouteRegistry

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("Route"))
typealias TaskFlowRoute = Route

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("RouteEntry"))
typealias TaskFlowRouteEntry = RouteEntry

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("NavRoutes"))
typealias TaskFlowNavRoutes = NavRoutes

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("ArticleNavRoutes"))
typealias TaskFlowArticleNavRoutes = ArticleNavRoutes

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("TaskNavRoutes"))
typealias TaskFlowTaskNavRoutes = TaskNavRoutes

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("LogNavRoutes"))
typealias TaskFlowLogNavRoutes = LogNavRoutes

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("LocalNavigator"))
val LocalTaskFlowNavigator = LocalNavigator

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberNavigator()"))
@Composable
fun rememberTaskFlowNavigator(): AppNavigator = rememberNavigator()

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("AppNavigator"))
typealias TaskFlowNavigator = AppNavigator

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberAppRouterInterceptorChain"))
@Composable
fun rememberTaskFlowAppRouterInterceptorChain(
    loginSession: com.example.zhttaskflow.nav.interceptor.LoginSession = rememberLoginSession(),
    deepLinkRouteMapper: com.example.zhttaskflow.nav.interceptor.DeepLinkRouteMapper = rememberDeepLinkRouteMapper(),
    permissionGrantChecker: com.example.zhttaskflow.nav.interceptor.PermissionGrantChecker = rememberPermissionGrantChecker(),
) = rememberAppRouterInterceptorChain(
    loginSession = loginSession,
    deepLinkRouteMapper = deepLinkRouteMapper,
    permissionGrantChecker = permissionGrantChecker,
)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberDeepLinkRouteMapper()"))
@Composable
fun rememberTaskFlowDeepLinkRouteMapper() = rememberDeepLinkRouteMapper()

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberRouterInterceptUiBridge(defaultErrorMessage)"))
@Composable
fun rememberTaskFlowRouterInterceptUiBridge(
    defaultErrorMessage: String,
) = rememberRouterInterceptUiBridge(defaultErrorMessage)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberLoginSession()"))
@Composable
fun rememberTaskFlowLoginSession() = rememberLoginSession()

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberLoginInterceptUi(loginSession)"))
@Composable
fun rememberTaskFlowLoginInterceptUi(
    loginSession: com.example.zhttaskflow.nav.interceptor.LoginSession,
) = rememberLoginInterceptUi(loginSession)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberPermissionGrantChecker(mode)"))
@Composable
fun rememberTaskFlowPermissionGrantChecker(
    mode: com.example.zhttaskflow.nav.interceptor.PermissionGrantCheckerMode = com.example.zhttaskflow.nav.interceptor.PermissionGrantCheckerMode.System,
) = rememberPermissionGrantChecker(mode)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberPermissionInterceptUi(grantChecker)"))
@Composable
fun rememberTaskFlowPermissionInterceptUi(
    grantChecker: com.example.zhttaskflow.nav.interceptor.PermissionGrantChecker = rememberPermissionGrantChecker(),
) = rememberPermissionInterceptUi(grantChecker)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("RouterInterceptor"))
typealias TaskFlowRouterInterceptor = RouterInterceptor

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("RouteRequest"))
typealias TaskFlowRouteRequest = RouteRequest

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("RouteInterceptResult"))
typealias TaskFlowRouteInterceptResult = RouteInterceptResult

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("RouterInterceptorChain"))
typealias TaskFlowRouterInterceptorChain = RouterInterceptorChain

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("LoginInterceptor"))
typealias TaskFlowLoginInterceptor = LoginInterceptor

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("PermissionInterceptor"))
typealias TaskFlowPermissionInterceptor = PermissionInterceptor

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("DeepLinkInterceptor"))
typealias TaskFlowDeepLinkInterceptor = DeepLinkInterceptor

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("DeepLinkNavigation"))
typealias TaskFlowDeepLinkNavigation = DeepLinkNavigation

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("RouteGatePolicy"))
typealias TaskFlowRouteGatePolicy = RouteGatePolicy

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("NavTransitionRegistry"))
typealias TaskFlowNavTransitionRegistry = NavTransitionRegistry

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("FeatureDebugDeepLinkState"))
typealias TaskFlowFeatureDebugDeepLinkState = com.example.zhttaskflow.nav.standalone.FeatureDebugDeepLinkState

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberFeatureDebugDeepLinkState()"))
@Composable
fun rememberTaskFlowFeatureDebugDeepLinkState() =
    com.example.zhttaskflow.nav.standalone.rememberFeatureDebugDeepLinkState()

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("FeatureDebugShell"))
@Composable
fun TaskFlowFeatureDebugShell(
    registry: RouteRegistry,
    startDestination: String,
    navigator: AppNavigator,
    mainTabRootRoute: String?,
    modifier: Modifier = Modifier,
    navController: androidx.navigation.NavHostController = rememberNavController(),
    routerInterceptorChain: RouterInterceptorChain? = null,
    deepLinkState: com.example.zhttaskflow.nav.standalone.FeatureDebugDeepLinkState? = null,
    analyticsImpl: com.example.zhttaskflow.base.analytics.Analytics? = null,
    performanceImpl: com.example.zhttaskflow.base.performance.PerformanceReporter? = null,
    crashReporterImpl: com.example.zhttaskflow.base.exception.CrashReporter? = null,
    loginSessionImpl: com.example.zhttaskflow.nav.interceptor.LoginSession? = null,
    deepLinkMapperImpl: com.example.zhttaskflow.nav.interceptor.DeepLinkRouteMapper? = null,
) = FeatureDebugShell(
    registry = registry,
    startDestination = startDestination,
    navigator = navigator,
    mainTabRootRoute = mainTabRootRoute,
    modifier = modifier,
    navController = navController,
    routerInterceptorChain = routerInterceptorChain,
    deepLinkState = deepLinkState,
    analyticsImpl = analyticsImpl,
    performanceImpl = performanceImpl,
    crashReporterImpl = crashReporterImpl,
    loginSessionImpl = loginSessionImpl,
    deepLinkMapperImpl = deepLinkMapperImpl,
)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("prepareFeatureDebug()"))
fun ComponentActivity.prepareTaskFlowFeatureDebug() = prepareFeatureDebug()
