package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
 * ## 标准用法（与 [BaseScaffold]、[PageScaffold] / [ListScaffold] 配合）
 * ```
 * ListScaffold(...) { _ ->
 *     StateBox(
 *         uiState = uiState,
 *         onRetry = { ... },
 *         contentPadding = rememberStateBoxContentPadding(),
 *         modifier = Modifier.fillMaxSize(),
 *     ) { data -> ... }
 * }
 * ```
 *
 * @param uiState 页面 MVI 状态
 * @param onRetry 错误态点击重试
 * @param modifier 根容器修饰符
 * @param contentPadding 在 [BaseScaffold] 内请使用 [rememberStateBoxContentPadding]（脚手架已消费 inset）
 * @param emptyMessage 空数据态展示文案
 * @param loading 首屏加载占位，默认居中 [androidx.compose.material3.CircularProgressIndicator]
 * @param content 成功态业务内容，参数为 [BaseUiState.Success.data]
 *
 * @see com.example.zhttaskflow.base.doc.BaseArchitecture
 */
@Composable
fun <T> StateBox(
    uiState: BaseUiState<T>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    emptyMessage: String = stringResource(id = R.string.base_str_empty),
    loading: @Composable (Modifier) -> Unit = { loadingModifier ->
        BaseLoadingScreen(modifier = loadingModifier)
    },
    content: @Composable (T) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        val branchModifier = Modifier.fillMaxSize()
        when (uiState) {
            BaseUiState.Loading -> {
                loading(branchModifier)
            }
            BaseUiState.Empty -> {
                BaseEmptyScreen(
                    message = emptyMessage,
                    modifier = branchModifier,
                )
            }
            is BaseUiState.Error -> {
                BaseErrorScreen(
                    message = uiState.message,
                    onRetry = onRetry,
                    modifier = branchModifier,
                )
            }
            is BaseUiState.Success -> {
                content(uiState.data)
            }
        }
    }
}
