package com.example.zhttaskflow.feature.home.presentation

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zhttaskflow.base.extension.collectUiStateWithLifecycle
import com.example.zhttaskflow.nav.LocalTaskFlowNavigator

/**
 * 首页路由注册 composable 入口：委托 [HomeRouteHost] 完成组装与渲染。
 */
@Composable
internal fun HomeRoute(
    onHomeBackPress: () -> Unit,
    taskListRoute: String?,
    articleListRoute: String?,
) {
    HomeRouteHost(
        onHomeBackPress = onHomeBackPress,
        taskListRoute = taskListRoute,
        articleListRoute = articleListRoute,
    )
}

/**
 * 首页路由宿主：组装 ViewModel、导航器与 [HomeScreen]，消费 [HomeUiEffect] 完成跳转。
 */
@Composable
private fun HomeRouteHost(
    onHomeBackPress: () -> Unit,
    taskListRoute: String?,
    articleListRoute: String?,
) {
    val context = LocalContext.current
    val navigator = LocalTaskFlowNavigator.current
    // UI 渲染层：仅获取 ViewModel 并挂载首页，不含业务逻辑
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

    val uiState by viewModel.uiState.collectUiStateWithLifecycle()
    HomeScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
    )
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
