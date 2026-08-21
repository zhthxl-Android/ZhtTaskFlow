package com.example.zhttaskflow.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.mvi.BaseViewModel

/**
 * 首页 ViewModel：MVI 单向数据流，负责入口列表状态与点击副作用下发。
 *
 * **调用方式**：UI 通过 [onEvent] 投递 [HomeUiEvent]；订阅 [uiState] 渲染，订阅 [uiEffect] 处理导航。
 *
 * **线程约束**：当前为本地固定入口数据，无异步 IO；后续接入 UseCase 时在 [launchTask] 内调度。
 */
internal class HomeViewModel(
    initialPageData: HomePageData,
    private val entranceRouteById: Map<String, String>,
) : BaseViewModel<HomeUiState, HomeUiEvent, HomeUiEffect>(
    initialState = BaseUiState.Success(initialPageData),
) {

    // region 初始化入口

    init {
        // 预留：后续可改为 setState(Loading) + launchTask { loadHomePageUseCase() }
    }

    // endregion

    // region 事件分发

    override fun handleEvent(event: HomeUiEvent) {
        when (event) {
            is HomeUiEvent.EntranceClicked -> onEntranceClicked(event.entranceId)
        }
    }

    // endregion

    // region 数据加载

    // 当前首页数据由路由层注入 [HomePageData]；无异步加载逻辑。

    // endregion

    // region 入口跳转

    private fun onEntranceClicked(entranceId: String) {
        val route = entranceRouteById[entranceId]
        if (route.isNullOrBlank()) {
            return
        }
        sendEffect(HomeUiEffect.NavigateToRoute(url = route))
    }

    // endregion
}

/**
 * [HomeViewModel] 手动注入工厂（无 Hilt）。
 */
internal class HomeViewModelFactory(
    private val homePageData: HomePageData,
    private val entranceRouteById: Map<String, String>,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                initialPageData = homePageData,
                entranceRouteById = entranceRouteById,
            ) as T
        }
        throw IllegalArgumentException("未知 ViewModel: ${modelClass.name}")
    }
}
