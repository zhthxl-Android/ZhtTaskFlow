package com.example.zhttaskflow.feature.task.presentation

import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.mvi.BaseUiEffect

/**
 * 任务列表页一次性副作用，不写入 [TaskUiState]。
 *
 * **消费分层**：
 * - 页面内 UI 反馈（如 [ShowSnackbar]）：由 [TaskListScreen] 消费
 * - 跨页面导航（[NavigateToEdit]）：由 [com.example.zhttaskflow.feature.task.navigation.TaskListRouteHost] 消费
 */
sealed interface TaskUiEffect : BaseUiEffect {

    /**
     * 展示全局 Snackbar（页面内 UI 反馈）。
     *
     * @param message 用户可读文案
     * @param type 成功 / 错误 / 普通样式，由业务层指定
     */
    data class ShowSnackbar(
        val message: String,
        val type: SnackbarType = SnackbarType.Normal,
    ) : TaskUiEffect

    /**
     * @deprecated 请改用 [ShowSnackbar]，并显式传入 [SnackbarType]。
     */
    @Deprecated(
        message = "已更名为 ShowSnackbar，请指定 type 参数",
        replaceWith = ReplaceWith(
            expression = "ShowSnackbar(message, SnackbarType.Normal)",
            imports = [
                "com.example.zhttaskflow.feature.task.presentation.TaskUiEffect.ShowSnackbar",
                "com.example.zhttaskflow.base.ext.SnackbarType",
            ],
        ),
    )
    data class ShowToast(val message: String) : TaskUiEffect

    /**
     * 跳转任务编辑/详情页（跨页面导航）。
     *
     * @param url 完整 Navigation 路由 path（由 [com.example.zhttaskflow.nav.route.TaskFlowTaskNavRoutes.detailPath] 生成）
     */
    data class NavigateToEdit(val url: String) : TaskUiEffect
}
