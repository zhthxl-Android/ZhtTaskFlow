package com.example.zhttaskflow.feature.log.domain.usecase

import com.example.zhttaskflow.feature.log.domain.repository.LogRepository

/**
 * 清空全部本地日志用例。
 */
class ClearLogsUseCase(
    private val repository: LogRepository,
) {

    suspend operator fun invoke() {
        repository.clearAllLogs()
    }
}
