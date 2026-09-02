package com.example.zhttaskflow.feature.log.domain.usecase

import com.example.zhttaskflow.feature.log.domain.LogPage
import com.example.zhttaskflow.feature.log.domain.LogQueryFilter
import com.example.zhttaskflow.feature.log.domain.repository.LogRepository

/**
 * 分页查询日志用例，支持按类型筛选。
 */
class QueryLogsUseCase(
    private val repository: LogRepository,
) {

    suspend operator fun invoke(
        filter: LogQueryFilter,
        page: Int,
        pageSize: Int,
    ): LogPage {
        return repository.queryPaged(filter, page, pageSize)
    }
}
