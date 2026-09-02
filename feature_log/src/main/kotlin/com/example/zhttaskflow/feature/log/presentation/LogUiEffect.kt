package com.example.zhttaskflow.feature.log.presentation

import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.ext.TaskFlowPresentationUiEffect
import com.example.zhttaskflow.base.mvi.BaseUiEffect
import java.io.File

/**
 * 日志查看页一次性副作用。
 *
 * 需 [androidx.compose.ui.res.stringResource] 的文案由 [ShowMessage] 在 Screen 层解析；
 * ViewModel 不持有 [android.content.Context]。
 */
sealed interface LogUiEffect : BaseUiEffect {

    /**
     * 打开系统分享面板导出日志文件（标题由 UI 层 string 资源 `log_str_export_share_title` 提供）。
     */
    data class ShowShareSheet(
        val exportFile: File,
    ) : LogUiEffect, TaskFlowPresentationUiEffect

    /**
     * 展示预定义用户文案（Screen 层映射为 string 资源）。
     */
    data class ShowMessage(
        val message: LogUserMessage,
        val type: SnackbarType = SnackbarType.Normal,
    ) : LogUiEffect, TaskFlowPresentationUiEffect

    /**
     * 展示动态文案 Snackbar（如用例/仓库返回的错误说明）。
     */
    data class ShowSnackbar(
        val message: String,
        val type: SnackbarType = SnackbarType.Normal,
    ) : LogUiEffect, TaskFlowPresentationUiEffect
}

/**
 * ViewModel 下发的固定用户提示键，由 [LogScreen] 映射为 string 资源。
 */
enum class LogUserMessage {
    /** 清空本地日志成功 */
    ClearLogsSuccess,
}
