package com.example.zhttaskflow.base

/**
 * 旧 `TaskFlow*` 公开 API 的 `@Deprecated` 过渡层（typealias / 转发 Composable）。
 *
 * **下个版本可统一移除**；新代码请使用各符号 `@Deprecated` 的 `ReplaceWith` 短名，勿新增对本文件的引用。
 */
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.zhttaskflow.base.ui.PageScaffold
import com.example.zhttaskflow.base.analytics.Analytics
import com.example.zhttaskflow.base.analytics.AnalyticsCompositionRoot
import com.example.zhttaskflow.base.analytics.LocalAnalytics
import com.example.zhttaskflow.base.analytics.PageViewEvent
import com.example.zhttaskflow.base.analytics.rememberAnalytics
import com.example.zhttaskflow.base.analytics.rememberDebugAnalytics
import com.example.zhttaskflow.base.exception.CrashReporter
import com.example.zhttaskflow.base.exception.CrashReporterCompositionRoot
import com.example.zhttaskflow.base.exception.LocalCrashReporter
import com.example.zhttaskflow.base.exception.rememberCrashReporter
import com.example.zhttaskflow.base.ext.LocalDialogController
import com.example.zhttaskflow.base.ext.LocalLoadingController
import com.example.zhttaskflow.base.ext.LocalSnackbarDispatcher
import com.example.zhttaskflow.base.ext.LocalSnackbarHostState
import com.example.zhttaskflow.base.ext.NavigationUiEffect
import com.example.zhttaskflow.base.ext.PresentationUiEffect
import com.example.zhttaskflow.base.ext.rememberDialogController
import com.example.zhttaskflow.base.ext.rememberLoadingController
import com.example.zhttaskflow.base.ext.rememberSnackbarDispatcher
import com.example.zhttaskflow.base.performance.LocalPerformance
import com.example.zhttaskflow.base.performance.Performance
import com.example.zhttaskflow.base.performance.PerformanceCompositionRoot
import com.example.zhttaskflow.base.performance.PerformanceReporter
import com.example.zhttaskflow.base.performance.createPerformance
import com.example.zhttaskflow.base.performance.rememberDebugPerformance
import com.example.zhttaskflow.base.performance.rememberPerformance
import com.example.zhttaskflow.base.theme.AppColors
import com.example.zhttaskflow.base.theme.LocalThemeController
import com.example.zhttaskflow.base.theme.ThemeController
import com.example.zhttaskflow.base.theme.ThemeMode
import com.example.zhttaskflow.base.theme.rememberThemeController
import androidx.compose.ui.unit.dp
import com.example.zhttaskflow.base.ext.handlePageBack
import com.example.zhttaskflow.base.ui.ImeAvoidanceMode
import com.example.zhttaskflow.base.ui.TopBarTitlePosition
import com.example.zhttaskflow.base.ui.UiConstants
import com.example.zhttaskflow.base.ui.rememberImePadding
import com.example.zhttaskflow.base.ui.rememberListLazyContentPadding
import com.example.zhttaskflow.base.ui.rememberScaffoldContentPadding
import com.example.zhttaskflow.base.ui.rememberStateBoxContentPadding

private const val DEPRECATION_MESSAGE = "将在下个版本移除，请使用新名称（见 ReplaceWith）"

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("Analytics"))
typealias TaskFlowAnalytics = Analytics

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("PageViewEvent"))
typealias TaskFlowPageViewEvent = PageViewEvent

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("CrashReporter"))
typealias TaskFlowCrashReporter = CrashReporter

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("Performance"))
typealias TaskFlowPerformance = Performance

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("PerformanceReporter"))
typealias TaskFlowPerformanceReporter = PerformanceReporter

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("NavigationUiEffect"))
typealias TaskFlowNavigationUiEffect = NavigationUiEffect

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("PresentationUiEffect"))
typealias TaskFlowPresentationUiEffect = PresentationUiEffect

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("TopBarTitlePosition"))
typealias TaskFlowTopBarTitlePosition = TopBarTitlePosition

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("ThemeMode"))
typealias TaskFlowThemeMode = ThemeMode

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("ThemeController"))
typealias TaskFlowThemeController = ThemeController

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("AppColors"))
typealias TaskFlowColors = AppColors

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("UiConstants"))
typealias TaskFlowUiConstants = UiConstants

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("LocalAnalytics"))
val LocalTaskFlowAnalytics = LocalAnalytics

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("LocalCrashReporter"))
val LocalTaskFlowCrashReporter = LocalCrashReporter

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("LocalPerformance"))
val LocalTaskFlowPerformance = LocalPerformance

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("LocalSnackbarHostState"))
val LocalTaskFlowSnackbarHostState = LocalSnackbarHostState

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("LocalSnackbarDispatcher"))
val LocalTaskFlowSnackbarDispatcher = LocalSnackbarDispatcher

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("LocalLoadingController"))
val LocalTaskFlowLoadingController = LocalLoadingController

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("LocalDialogController"))
val LocalTaskFlowDialogController = LocalDialogController

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("LocalThemeController"))
val LocalTaskFlowThemeController = LocalThemeController

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberAnalytics()"))
@Composable
fun rememberTaskFlowAnalytics(): Analytics = rememberAnalytics()

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberDebugAnalytics()"))
@Composable
fun rememberTaskFlowDebugAnalytics(): Analytics = rememberDebugAnalytics()

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberCrashReporter()"))
@Composable
fun rememberTaskFlowCrashReporter(
    override: CrashReporter? = null,
): CrashReporter = rememberCrashReporter(override)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberPerformance()"))
@Composable
fun rememberTaskFlowPerformance(): Performance = rememberPerformance()

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberDebugPerformance(reporter)"))
@Composable
fun rememberTaskFlowDebugPerformance(
    reporter: PerformanceReporter = com.example.zhttaskflow.base.performance.DebugPerformanceReporter,
): Performance = rememberDebugPerformance(reporter)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("createPerformance(reporter)"))
fun createTaskFlowPerformance(
    reporter: PerformanceReporter = com.example.zhttaskflow.base.performance.DebugPerformanceReporter,
): Performance = createPerformance(reporter)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("AnalyticsCompositionRoot"))
@Composable
fun TaskFlowAnalyticsCompositionRoot(
    analytics: Analytics = rememberDebugAnalytics(),
    content: @Composable () -> Unit,
) = AnalyticsCompositionRoot(analytics, content)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("PerformanceCompositionRoot"))
@Composable
fun TaskFlowPerformanceCompositionRoot(
    performance: Performance = rememberDebugPerformance(),
    content: @Composable () -> Unit,
) = PerformanceCompositionRoot(performance, content)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("CrashReporterCompositionRoot"))
@Composable
fun TaskFlowCrashReporterCompositionRoot(
    crashReporter: CrashReporter = rememberCrashReporter(),
    content: @Composable () -> Unit,
) = CrashReporterCompositionRoot(crashReporter, content)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberSnackbarDispatcher()"))
@Composable
fun rememberTaskFlowSnackbarDispatcher() = rememberSnackbarDispatcher()

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberDialogController()"))
@Composable
fun rememberTaskFlowDialogController() = rememberDialogController()

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberLoadingController()"))
@Composable
fun rememberTaskFlowLoadingController() = rememberLoadingController()

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberThemeController()"))
@Composable
fun rememberTaskFlowThemeController(
    initialMode: ThemeMode = ThemeMode.FollowSystem,
) = rememberThemeController(initialMode)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberScaffoldContentPadding(scaffoldPadding)"))
@Composable
fun rememberTaskFlowScaffoldContentPadding(
    scaffoldPadding: androidx.compose.foundation.layout.PaddingValues,
) = rememberScaffoldContentPadding(scaffoldPadding)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberStateBoxContentPadding()"))
@Composable
fun rememberTaskFlowStateBoxContentPadding() = rememberStateBoxContentPadding()

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberListLazyContentPadding(scaffoldPadding, extraBottom)"))
@Composable
fun rememberTaskFlowListLazyContentPadding(
    scaffoldPadding: androidx.compose.foundation.layout.PaddingValues,
    extraBottom: androidx.compose.ui.unit.Dp = 0.dp,
) = rememberListLazyContentPadding(scaffoldPadding, extraBottom)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("ImeAvoidanceMode"))
typealias TaskFlowImeAvoidanceMode = ImeAvoidanceMode

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("rememberImePadding(mode)"))
@Composable
fun rememberTaskFlowImePadding(
    mode: ImeAvoidanceMode,
) = rememberImePadding(mode)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("handlePageBack(onNavigateUp, onBackIntercept)"))
fun handleTaskFlowPageBack(
    onNavigateUp: () -> Unit,
    onBackIntercept: (() -> Boolean)? = null,
) = handlePageBack(onNavigateUp, onBackIntercept)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("UiEffectConsumption"))
typealias TaskFlowUiEffectConsumption = com.example.zhttaskflow.base.ext.UiEffectConsumption

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("AppIcons"))
typealias TaskFlowIcons = com.example.zhttaskflow.base.ui.icon.AppIcons

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("DividerStyle"))
typealias TaskFlowDividerStyle = com.example.zhttaskflow.base.ui.DividerStyle

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("Divider"))
@Composable
fun TaskFlowDivider(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    style: com.example.zhttaskflow.base.ui.DividerStyle = com.example.zhttaskflow.base.ui.DividerStyle.List,
) = com.example.zhttaskflow.base.ui.Divider(modifier = modifier, style = style)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("SnackbarType"))
typealias TaskFlowSnackbarType = com.example.zhttaskflow.base.ui.SnackbarType

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("SnackbarVisuals"))
typealias TaskFlowSnackbarVisuals = com.example.zhttaskflow.base.ui.SnackbarVisuals

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("SnackbarHost"))
@Composable
fun TaskFlowSnackbarHost(
    hostState: androidx.compose.material3.SnackbarHostState,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
) = com.example.zhttaskflow.base.ui.SnackbarHost(hostState = hostState, modifier = modifier)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("PullToRefreshBox"))
@Composable
fun TaskFlowPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    content: @androidx.compose.runtime.Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) = com.example.zhttaskflow.base.ui.PullToRefreshBox(
    isRefreshing = isRefreshing,
    onRefresh = onRefresh,
    modifier = modifier,
    content = content,
)

@Deprecated(DEPRECATION_MESSAGE, ReplaceWith("PageScaffold"))
@Composable
fun TaskFlowScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Unit)? = null,
    onBackIntercept: (() -> Boolean)? = null,
    enableSystemBackHandler: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    PageScaffold(
        title = title,
        modifier = modifier,
        onNavigateUp = onNavigateUp,
        onBackIntercept = onBackIntercept,
        enableSystemBackHandler = enableSystemBackHandler,
        navigationIcon = navigationIcon,
        actions = actions,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        content = content,
    )
}

