package com.example.zhttaskflow.feature.log.domain

/**
 * 日志导出范围：与列表筛选一致，或忽略类型筛选导出全部。
 */
enum class LogExportScope {
    /** 导出当前 UI 筛选条件下的日志（默认）。 */
    CURRENT_FILTER,

    /** 导出全部类型日志。 */
    ALL_LOGS,
}
