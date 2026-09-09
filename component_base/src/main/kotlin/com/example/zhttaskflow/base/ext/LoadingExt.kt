package com.example.zhttaskflow.base.ext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 阻塞加载弹窗 UI 状态。
 */
data class LoadingUiState(
    val visible: Boolean = false,
    val message: String? = null,
)

/**
 * 由 [com.example.zhttaskflow.base.ui.BaseScaffold] 注入的全局加载控制器。
 */
val LocalLoadingController = compositionLocalOf<LoadingController> {
    error("LoadingController 未提供，请使用 BaseScaffold 包裹页面")
}

/**
 * 控制全屏阻塞加载显隐；页面离开组合时由脚手架自动 [hideLoading]。
 */
@Stable
class LoadingController internal constructor() {
    var uiState by mutableStateOf(LoadingUiState())
        private set

    fun showLoading(message: String? = null) {
        uiState = LoadingUiState(visible = true, message = message)
    }

    fun hideLoading() {
        uiState = LoadingUiState(visible = false, message = null)
    }
}

@Composable
fun rememberLoadingController(): LoadingController {
    return LocalLoadingController.current
}

/**
 * 顶层便捷：展示阻塞加载（[message] 为空时使用默认文案）。
 */
fun showLoading(controller: LoadingController, message: String? = null) {
    controller.showLoading(message = message)
}

/**
 * 顶层便捷：关闭阻塞加载。
 */
fun hideLoading(controller: LoadingController) {
    controller.hideLoading()
}
