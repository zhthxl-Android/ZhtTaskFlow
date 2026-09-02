package com.example.zhttaskflow.feature.log.presentation

import com.example.zhttaskflow.base.mvi.BaseUiEvent
import com.example.zhttaskflow.feature.log.domain.LogExportScope

/**
 * 日志查看页用户事件（唯一 UI 输入）。
 */
sealed interface LogUiEvent : BaseUiEvent {

    /** 首屏 / 重试加载本地日志。 */
    data object Load : LogUiEvent

    /** StateBox 错误态重试。 */
    data object Retry : LogUiEvent

    /** 切换顶部类型筛选。 */
    data class FilterSelected(val filter: LogTypeFilter) : LogUiEvent

    /** 点击列表项展开 / 收起详情。 */
    data class EntryToggled(val entryId: String) : LogUiEvent

    /** 用户确认导出范围后执行导出并唤起系统分享。 */
    data class ExportConfirmed(
        val scope: LogExportScope,
    ) : LogUiEvent

    /** 请求清空（由 UI 二次确认后再发 [ClearConfirmed]）。 */
    data object ClearRequested : LogUiEvent

    /** 确认清空全部本地日志。 */
    data object ClearConfirmed : LogUiEvent

    /** 列表滚动触底，加载下一页。 */
    data object LoadMore : LogUiEvent
}
