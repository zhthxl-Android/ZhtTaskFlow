package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.zhttaskflow.base.ext.LocalTaskFlowLoadingController
import com.example.zhttaskflow.base.ext.LocalTaskFlowDialogController
import com.example.zhttaskflow.base.ext.LocalTaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ext.LocalTaskFlowSnackbarHostState
import com.example.zhttaskflow.base.ext.TaskFlowDialogController
import com.example.zhttaskflow.base.ext.TaskFlowLoadingController
import com.example.zhttaskflow.base.ext.TaskFlowSnackbarDispatcher
import com.example.zhttaskflow.base.ui.dialog.TaskFlowDialogHost

/**
 * 核心页面脚手架：统一 [TaskFlowInsetsPolicy]、系统栏与内容区边距，不含顶栏/导航等业务层级 UI。
 *
 * 内置全局 Snackbar、阻塞加载与确认/底部弹窗宿主及对应 CompositionLocal。
 */
@Composable
fun TaskFlowBaseScaffold(
    modifier: Modifier = Modifier,
    consumeStatusBarsInContent: Boolean,
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    header: @Composable () -> Unit = {},
    contentModifier: Modifier = Modifier,
    content: @Composable (scaffoldContentPadding: PaddingValues) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val snackbarDispatcher = remember(snackbarHostState, snackbarScope) {
        TaskFlowSnackbarDispatcher(
            hostState = snackbarHostState,
            scope = snackbarScope,
        )
    }
    val loadingController = remember { TaskFlowLoadingController() }
    val dialogController = remember { TaskFlowDialogController() }

    DisposableEffect(loadingController) {
        onDispose { loadingController.hideLoading() }
    }
    DisposableEffect(dialogController) {
        onDispose { dialogController.dismissAll() }
    }

    CompositionLocalProvider(
        LocalTaskFlowSnackbarHostState provides snackbarHostState,
        LocalTaskFlowSnackbarDispatcher provides snackbarDispatcher,
        LocalTaskFlowLoadingController provides loadingController,
        LocalTaskFlowDialogController provides dialogController,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = TaskFlowInsetsPolicy.scaffoldContentWindowInsets,
                bottomBar = bottomBar,
                floatingActionButton = floatingActionButton,
                snackbarHost = { TaskFlowSnackbarHost(hostState = snackbarHostState) },
            ) { innerPadding ->
                val contentInsets = rememberTaskFlowScaffoldContentPadding(scaffoldPadding = innerPadding)
                Column(modifier = Modifier.fillMaxSize()) {
                    header()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .then(
                                if (consumeStatusBarsInContent) {
                                    Modifier.statusBarsPadding()
                                } else {
                                    Modifier
                                },
                            )
                            .then(contentModifier)
                            .padding(contentInsets),
                    ) {
                        content(contentInsets)
                    }
                }
            }
            val loadingState = loadingController.uiState
            TaskFlowBlockingLoadingOverlay(
                visible = loadingState.visible,
                message = loadingState.message,
            )
            TaskFlowDialogHost(controller = dialogController)
        }
    }
}
