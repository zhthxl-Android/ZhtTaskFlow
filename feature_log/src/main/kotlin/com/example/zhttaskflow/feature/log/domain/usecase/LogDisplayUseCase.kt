package com.example.zhttaskflow.feature.log.domain.usecase

import com.example.zhttaskflow.feature.log.domain.LogEntry
import com.example.zhttaskflow.feature.log.domain.repository.LogRepository

/**
 * 日志列表展示格式化（时间列、展开详情 JSON）。
 */
class LogDisplayUseCase(
    private val repository: LogRepository,
) {

    fun formatTimestamp(epochMs: Long): String = repository.formatTimestamp(epochMs)

    fun encodeDetail(entry: LogEntry): String = repository.encodeLogDetail(entry)
}
