package com.example.zhttaskflow.feature.log.presentation

import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.ext.TaskFlowPresentationUiEffect
import com.example.zhttaskflow.base.mvi.BaseUiEffect
import java.io.File

/**
 * 日志查看页一次性副作用。
 */
sealed interface LogUiEffect : BaseUiEffect {

    /**
     * 通过系统分享面板导出日志文件。
     */
    data class ShareLogExport(
        val exportFile: File,
        val chooserTitle: String,
    ) : LogUiEffect, TaskFlowPresentationUiEffect

    /**
     * 页面内 Snackbar 反馈。
     */
    data class ShowSnackbar(
        val message: String,
        val type: SnackbarType = SnackbarType.Normal,
    ) : LogUiEffect, TaskFlowPresentationUiEffect
}
