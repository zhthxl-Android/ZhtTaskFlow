package com.example.zhttaskflow.feature.log.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zhttaskflow.feature.log.data.repository.LogRepositoryImpl
import com.example.zhttaskflow.feature.log.domain.repository.LogRepository
import com.example.zhttaskflow.feature.log.domain.usecase.ClearLogsUseCase
import com.example.zhttaskflow.feature.log.domain.usecase.ExportLogsUseCase
import com.example.zhttaskflow.feature.log.domain.usecase.LogDisplayUseCase
import com.example.zhttaskflow.feature.log.domain.usecase.QueryLogsUseCase
import com.example.zhttaskflow.feature.log.presentation.LogScreen
import com.example.zhttaskflow.feature.log.presentation.LogViewModel
import com.example.zhttaskflow.feature.log.presentation.LogViewModelFactory
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowLogNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry
import com.example.zhttaskflow.nav.route.simpleRouteEntry

/**
 * 日志模块路由注册入口（常量见 [TaskFlowLogNavRoutes]）。
 *
 * 门禁：`app/log` 无标记，见 `docs/TASKFLOW_ROUTE_GATES.md`。
 */
fun registerLogRoutes(
    registry: TaskFlowRouteRegistry,
    /** 与全局路由注册签名对齐。 */
    @Suppress("UNUSED_PARAMETER") navigator: TaskFlowNavigator,
) {
    registry.register(
        simpleRouteEntry(
            route = TaskFlowLogNavRoutes.LOG_ROUTE,
            content = { LogRouteHost() },
        ),
    )
}

@Composable
private fun LogRouteHost() {
    val factory = rememberLogViewModelFactory()
    val viewModel: LogViewModel = viewModel(factory = factory)
    LogScreen(viewModel = viewModel)
}

@Composable
private fun rememberLogRepository(): LogRepository {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) {
        LogRepositoryImpl(appContext)
    }
}

@Composable
private fun rememberLogViewModelFactory(): LogViewModelFactory {
    val repository = rememberLogRepository()
    return remember(repository) {
        LogViewModelFactory(
            queryLogsUseCase = QueryLogsUseCase(repository),
            exportLogsUseCase = ExportLogsUseCase(repository),
            clearLogsUseCase = ClearLogsUseCase(repository),
            logDisplayUseCase = LogDisplayUseCase(repository),
        )
    }
}
