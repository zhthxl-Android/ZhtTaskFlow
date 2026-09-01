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
data class TaskFlowLoadingUiState(
    val visible: Boolean = false,
    val message: String? = null,
)

/**
 * 由 [com.example.zhttaskflow.base.ui.TaskFlowBaseScaffold] 注入的全局加载控制器。
 */
val LocalTaskFlowLoadingController = compositionLocalOf<TaskFlowLoadingController> {
    error("TaskFlowLoadingController 未提供，请使用 TaskFlowBaseScaffold 包裹页面")
}

/**
 * 控制全屏阻塞加载显隐；页面离开组合时由脚手架自动 [hideLoading]。
 */
@Stable
class TaskFlowLoadingController internal constructor() {
    var uiState by mutableStateOf(TaskFlowLoadingUiState())
        private set

    fun showLoading(message: String? = null) {
        uiState = TaskFlowLoadingUiState(visible = true, message = message)
    }

    fun hideLoading() {
        uiState = TaskFlowLoadingUiState(visible = false, message = null)
    }
}

@Composable
fun rememberTaskFlowLoadingController(): TaskFlowLoadingController {
    return LocalTaskFlowLoadingController.current
}

/**
 * 顶层便捷：展示阻塞加载（[message] 为空时使用默认文案）。
 */
fun showLoading(controller: TaskFlowLoadingController, message: String? = null) {
    controller.showLoading(message = message)
}

/**
 * 顶层便捷：关闭阻塞加载。
 */
fun hideLoading(controller: TaskFlowLoadingController) {
    controller.hideLoading()
}
