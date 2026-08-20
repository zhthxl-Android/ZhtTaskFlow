package com.example.zhttaskflow.feature.home.presentation

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
 * 首页路由宿主：组装 ViewModel、导航器与 [HomeScreen]，消费 [HomeUiEffect] 完成跳转。
 */
@Composable
internal fun HomeRoute(
    onHomeBackPress: () -> Unit,
    taskListRoute: String?,
    articleListRoute: String?,
) {
    val context = LocalContext.current
    val navigator = LocalTaskFlowNavigator.current
    val appContext = context.applicationContext
    val factory = remember(appContext, taskListRoute, articleListRoute) {
        HomeViewModelFactory(
            homePageData = HomePresentationDefaults.buildFixedHomePageData(appContext),
            entranceRouteById = HomePresentationDefaults.buildEntranceRouteMap(
                taskListRoute = taskListRoute,
                articleListRoute = articleListRoute,
            ),
        )
    }
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
