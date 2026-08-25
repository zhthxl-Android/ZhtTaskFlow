package com.example.zhttaskflow.feature.home.navigation

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.zhttaskflow.feature.home.presentation.HomePresentationDefaults
import com.example.zhttaskflow.feature.home.presentation.HomeScreen
import com.example.zhttaskflow.feature.home.presentation.HomeUiEffect
import com.example.zhttaskflow.feature.home.presentation.HomeViewModel
import com.example.zhttaskflow.feature.home.presentation.HomeViewModelFactory
import com.example.zhttaskflow.nav.LocalTaskFlowNavigator
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowHomeNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowRouteEntry
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry

/**
 * 在 [TaskFlowRouteRegistry] 中注册首页路由（与 `registerTaskRoutes` / `registerArticleRoutes` 范式一致）。
 *
 * 模块对外唯一入口；路由常量统一使用 [TaskFlowHomeNavRoutes.HOME_ROUTE]。
 *
 * @param taskListRoute 任务列表路由 path；standalone 或未装配任务模块时可传 null
 * @param articleListRoute 资讯列表路由 path；同上
 * @param onHomeBackPress 首页系统返回；standalone 可传 `activity.finish()`
 */
fun registerHomeRoutes(
    registry: TaskFlowRouteRegistry,
    /** 与全局路由注册签名对齐，导航由 Composable 内 [LocalTaskFlowNavigator] 消费。 */
    @Suppress("UNUSED_PARAMETER") navigator: TaskFlowNavigator,
    taskListRoute: String? = null,
    articleListRoute: String? = null,
    onHomeBackPress: () -> Unit = {},
) {
    registry.register(
        TaskFlowRouteEntry(
            route = TaskFlowHomeNavRoutes.HOME_ROUTE,
            register = {
                registerHomeRoutes(
                    taskListRoute = taskListRoute,
                    articleListRoute = articleListRoute,
                    onHomeBackPress = onHomeBackPress,
                )
            },
        ),
    )
}

/**
 * 在 NavGraph 上注册首页 composable（由 [registerHomeRoutes] 经 [TaskFlowRouteEntry] 调用，不对外暴露）。
 */
internal fun NavGraphBuilder.registerHomeRoutes(
    taskListRoute: String? = null,
    articleListRoute: String? = null,
    onHomeBackPress: () -> Unit = {},
) {
    composable(route = TaskFlowHomeNavRoutes.HOME_ROUTE) {
        HomeRouteHost(
            onHomeBackPress = onHomeBackPress,
            taskListRoute = taskListRoute,
            articleListRoute = articleListRoute,
        )
    }
}

/**
 * 首页路由宿主：组装 ViewModel、导航器与 [HomeScreen]；仅消费跨页面导航类 [HomeUiEffect]。
 */
@Composable
private fun HomeRouteHost(
    onHomeBackPress: () -> Unit,
    taskListRoute: String?,
    articleListRoute: String?,
) {
    val context = LocalContext.current
    val navigator = LocalTaskFlowNavigator.current
    val factory = rememberHomeViewModelFactory(
        appContext = context.applicationContext,
        taskListRoute = taskListRoute,
        articleListRoute = articleListRoute,
    )
    val viewModel: HomeViewModel = viewModel(factory = factory)

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is HomeUiEffect.NavigateToRoute -> {
                    navigator.navigate(effect.url)
                }
            }
        }
    }

    BackHandler(onBack = onHomeBackPress)

    HomeScreen(viewModel = viewModel)
}

/**
 * 依赖组装层：固定首页数据 → 入口路由表 → [HomeViewModelFactory]。
 *
 * 不包含业务逻辑；[remember] 缓存 key 与生命周期与原 [HomeRoute] 内联实现一致。
 */
@Composable
private fun rememberHomeViewModelFactory(
    appContext: Context,
    taskListRoute: String?,
    articleListRoute: String?,
): HomeViewModelFactory {
    return remember(appContext, taskListRoute, articleListRoute) {
        HomeViewModelFactory(
            homePageData = HomePresentationDefaults.buildFixedHomePageData(appContext),
            entranceRouteById = HomePresentationDefaults.buildEntranceRouteMap(
                taskListRoute = taskListRoute,
                articleListRoute = articleListRoute,
            ),
        )
    }
}
