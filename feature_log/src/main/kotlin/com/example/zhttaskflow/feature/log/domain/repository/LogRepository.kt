package com.example.zhttaskflow.feature.log.domain.repository

import com.example.zhttaskflow.feature.log.domain.LogEntry
import com.example.zhttaskflow.feature.log.domain.LogPage
import com.example.zhttaskflow.feature.log.domain.LogQueryFilter
import java.io.File

/**
 * 日志仓库抽象：查询、导出、清空及展示格式化，由 data 层实现。
 */
interface LogRepository {

    /**
     * 按页查询日志（时间倒序）。
     */
    suspend fun queryPaged(
        filter: LogQueryFilter,
        page: Int,
        pageSize: Int,
    ): LogPage

    /**
     * 按筛选条件导出 JSONL 文件。
     */
    suspend fun exportLogs(
        filter: LogQueryFilter,
        maxEntries: Int = 2_000,
    ): File

    /** 清空本地全部日志。 */
    suspend fun clearAllLogs()

    /** 列表时间列展示文案。 */
    fun formatTimestamp(epochMs: Long): String

    /** 展开详情用的 JSON 文本。 */
    fun encodeLogDetail(entry: LogEntry): String
}
