package com.example.zhttaskflow.base.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.zhttaskflow.base.R
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.ui.state.BaseEmptyScreen
import com.example.zhttaskflow.base.ui.state.BaseErrorScreen
import com.example.zhttaskflow.base.ui.state.BaseLoadingScreen

/**
 * 基于 [BaseUiState] 的通用页面状态容器：统一 Loading / Empty / Error / Success 四类表现。
 *
 * ## 用途
 * 收敛业务页面重复的 `when (uiState)` 分支，成功态业务 UI 通过 [content] 插槽渲染。
 *
 * ## 标准用法
 * ```
 * StateBox(
 *     uiState = uiState,
 *     onRetry = { viewModel.onEvent(Refresh) },
 *     emptyMessage = stringResource(R.string.xxx_empty),
 *     modifier = Modifier.fillMaxSize().padding(innerPadding),
 * ) { data ->
 *     // 仅编写 Success 态业务内容（列表、表单等）
 * }
 * ```
 *
 * @param uiState 页面 MVI 状态
 * @param onRetry 错误态点击重试
 * @param modifier 作用于当前展示分支的根布局（含 Loading / Empty / Error / Success）
 * @param emptyMessage 空数据态展示文案
 * @param loading 首屏加载占位，默认居中 [androidx.compose.material3.CircularProgressIndicator]
 * @param content 成功态业务内容，参数为 [BaseUiState.Success.data]
 */
@Composable
fun <T> StateBox(
    uiState: BaseUiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyMessage: String = stringResource(id = R.string.base_str_empty),
    loading: @Composable (Modifier) -> Unit = { loadingModifier ->
        BaseLoadingScreen(modifier = loadingModifier)
    },
    content: @Composable (T) -> Unit,
) {
    when (uiState) {
        BaseUiState.Loading -> {
            loading(modifier)
        }
        BaseUiState.Empty -> {
            BaseEmptyScreen(
                message = emptyMessage,
                modifier = modifier,
            )
        }
        is BaseUiState.Error -> {
            BaseErrorScreen(
                message = uiState.message,
                onRetry = onRetry,
                modifier = modifier,
            )
        }
        is BaseUiState.Success -> {
            content(uiState.data)
        }
    }
}
