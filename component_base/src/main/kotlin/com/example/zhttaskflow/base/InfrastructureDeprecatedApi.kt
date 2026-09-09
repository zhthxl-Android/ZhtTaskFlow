package com.example.zhttaskflow.base

/**
 * 列表 / 异常 / 弹窗等基建旧名的 `@Deprecated` 过渡（如 `TaskFlowBlockingLoadingOverlay` → [BlockingLoadingOverlay]）。
 *
 * **下个版本可统一移除**；请改用各符号 `ReplaceWith` 中的新 API。
 */
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.zhttaskflow.base.exception.CrashReporter
import com.example.zhttaskflow.base.exception.ExceptionHandler
import com.example.zhttaskflow.base.exception.ExceptionMonitoringRoot
import com.example.zhttaskflow.base.exception.NetworkOfflineBanner
import com.example.zhttaskflow.base.ext.DialogPresentation
import com.example.zhttaskflow.base.ext.LoadingUiState
import com.example.zhttaskflow.base.ext.PageBackHandler
import com.example.zhttaskflow.base.ext.SnackbarDispatcher
import com.example.zhttaskflow.base.ext.TabRootBackHandler
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.theme.NoRippleContent
import com.example.zhttaskflow.base.theme.RippleTokens
import com.example.zhttaskflow.base.ui.BlockingLoadingOverlay
import com.example.zhttaskflow.base.ui.ImePaddingState
import com.example.zhttaskflow.base.ui.ImmersiveStatusBarEffect
import com.example.zhttaskflow.base.ui.InsetsPolicy
import com.example.zhttaskflow.base.ui.ListLoadMoreFooter
import com.example.zhttaskflow.base.ui.ListPaginationState
import com.example.zhttaskflow.base.ui.ListSkeletonLoading
import com.example.zhttaskflow.base.ui.PageBackground
import com.example.zhttaskflow.base.ui.PaginatedListPayload
import com.example.zhttaskflow.base.ui.PaginationController
import com.example.zhttaskflow.base.ui.RefreshableListPayload
import com.example.zhttaskflow.base.ui.StatePaginatedListContent
import com.example.zhttaskflow.base.ui.StateRefreshableListContent
import com.example.zhttaskflow.base.ui.UiConstants
import com.example.zhttaskflow.base.ui.dialog.BottomSheet
import com.example.zhttaskflow.base.ui.dialog.ConfirmDialog
import com.example.zhttaskflow.base.ui.imePadding
import com.example.zhttaskflow.base.ui.skeleton.SkeletonTemplate

private const val INFRA_DEPRECATION = "将在下个版本移除，请使用新名称（见 ReplaceWith）"

@Deprecated(INFRA_DEPRECATION, ReplaceWith("ListPaginationState"))
typealias TaskFlowListPaginationState = ListPaginationState

@Deprecated(INFRA_DEPRECATION, ReplaceWith("PaginatedListPayload"))
typealias TaskFlowPaginatedListPayload<T> = PaginatedListPayload<T>

@Deprecated(INFRA_DEPRECATION, ReplaceWith("RefreshableListPayload"))
typealias TaskFlowRefreshableListPayload<T> = RefreshableListPayload<T>

@Deprecated(INFRA_DEPRECATION, ReplaceWith("PaginationController"))
typealias TaskFlowPaginationController = PaginationController

@Deprecated(INFRA_DEPRECATION, ReplaceWith("LoadingUiState"))
typealias TaskFlowLoadingUiState = LoadingUiState

@Deprecated(INFRA_DEPRECATION, ReplaceWith("DialogPresentation"))
typealias TaskFlowDialogPresentation = DialogPresentation

@Deprecated(INFRA_DEPRECATION, ReplaceWith("ImePaddingState"))
typealias TaskFlowImePaddingState = ImePaddingState

@Deprecated(INFRA_DEPRECATION, ReplaceWith("InsetsPolicy"))
typealias TaskFlowInsetsPolicy = InsetsPolicy

@Deprecated(INFRA_DEPRECATION, ReplaceWith("PageBackground"))
typealias TaskFlowPageBackground = PageBackground

@Deprecated(INFRA_DEPRECATION, ReplaceWith("RippleTokens"))
typealias TaskFlowRippleTokens = RippleTokens

@Deprecated(INFRA_DEPRECATION, ReplaceWith("SkeletonTemplate"))
typealias TaskFlowSkeletonTemplate = SkeletonTemplate

@Deprecated(INFRA_DEPRECATION, ReplaceWith("ExceptionHandler"))
typealias TaskFlowExceptionHandler = ExceptionHandler

@Deprecated(INFRA_DEPRECATION, ReplaceWith("StatePaginatedListContent"))
@Composable
fun <T> TaskFlowStatePaginatedListContent(
    uiState: BaseUiState<PaginatedListPayload<T>>,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    listContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onRetryLoadMore: () -> Unit = onLoadMore,
    contentPadding: PaddingValues = PaddingValues(horizontal = UiConstants.PageHorizontalPadding),
    emptyMessage: String = androidx.compose.ui.res.stringResource(
        id = com.example.zhttaskflow.base.R.string.base_str_empty,
    ),
    skeletonTemplate: SkeletonTemplate = SkeletonTemplate.List,
    skeletonItemCount: Int = UiConstants.SkeletonDefaultListItemCount,
    loading: @Composable (Modifier) -> Unit = { loadingModifier ->
        ListSkeletonLoading(
            listContentPadding = listContentPadding,
            modifier = loadingModifier,
            itemCount = skeletonItemCount,
            template = skeletonTemplate,
        )
    },
    key: ((index: Int, item: T) -> Any)? = null,
    itemContent: @Composable (index: Int, item: T) -> Unit,
) = StatePaginatedListContent(
    uiState = uiState,
    onRetry = onRetry,
    onRefresh = onRefresh,
    onLoadMore = onLoadMore,
    listContentPadding = listContentPadding,
    modifier = modifier,
    onRetryLoadMore = onRetryLoadMore,
    contentPadding = contentPadding,
    emptyMessage = emptyMessage,
    skeletonTemplate = skeletonTemplate,
    skeletonItemCount = skeletonItemCount,
    loading = loading,
    key = key,
    itemContent = itemContent,
)

@Deprecated(INFRA_DEPRECATION, ReplaceWith("StateRefreshableListContent"))
@Composable
fun <T> TaskFlowStateRefreshableListContent(
    uiState: BaseUiState<RefreshableListPayload<T>>,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    listContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = UiConstants.PageHorizontalPadding),
    emptyMessage: String = androidx.compose.ui.res.stringResource(
        id = com.example.zhttaskflow.base.R.string.base_str_empty,
    ),
    skeletonTemplate: SkeletonTemplate = SkeletonTemplate.List,
    skeletonItemCount: Int = UiConstants.SkeletonDefaultListItemCount,
    loading: @Composable (Modifier) -> Unit = { loadingModifier ->
        ListSkeletonLoading(
            listContentPadding = listContentPadding,
            modifier = loadingModifier,
            itemCount = skeletonItemCount,
            template = skeletonTemplate,
        )
    },
    key: ((index: Int, item: T) -> Any)? = null,
    itemContent: @Composable (index: Int, item: T) -> Unit,
) = StateRefreshableListContent(
    uiState = uiState,
    onRetry = onRetry,
    onRefresh = onRefresh,
    listContentPadding = listContentPadding,
    modifier = modifier,
    contentPadding = contentPadding,
    emptyMessage = emptyMessage,
    skeletonTemplate = skeletonTemplate,
    skeletonItemCount = skeletonItemCount,
    loading = loading,
    key = key,
    itemContent = itemContent,
)

@Deprecated(INFRA_DEPRECATION, ReplaceWith("ListLoadMoreFooter"))
@Composable
fun TaskFlowListLoadMoreFooter(
    pagination: ListPaginationState,
    onRetryLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) = ListLoadMoreFooter(pagination, onRetryLoadMore, modifier)

@Deprecated(INFRA_DEPRECATION, ReplaceWith("ListSkeletonLoading"))
@Composable
fun TaskFlowListSkeletonLoading(
    listContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    itemCount: Int = UiConstants.SkeletonDefaultListItemCount,
    template: SkeletonTemplate = SkeletonTemplate.List,
) = ListSkeletonLoading(listContentPadding, modifier, itemCount, template)

@Deprecated(INFRA_DEPRECATION, ReplaceWith("ExceptionMonitoringRoot"))
@Composable
fun TaskFlowExceptionMonitoringRoot(
    snackbarDispatcher: SnackbarDispatcher?,
    crashReporter: CrashReporter? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = ExceptionMonitoringRoot(
    snackbarDispatcher = snackbarDispatcher,
    crashReporter = crashReporter,
    modifier = modifier,
    content = content,
)

@Deprecated(INFRA_DEPRECATION, ReplaceWith("NetworkOfflineBanner"))
@Composable
fun TaskFlowNetworkOfflineBanner(
    modifier: Modifier = Modifier,
) = NetworkOfflineBanner(modifier)

@Deprecated(INFRA_DEPRECATION, ReplaceWith("BlockingLoadingOverlay"))
@Composable
fun TaskFlowBlockingLoadingOverlay(
    visible: Boolean,
    message: String?,
    modifier: Modifier = Modifier,
) = BlockingLoadingOverlay(visible, message, modifier)

@Deprecated(INFRA_DEPRECATION, ReplaceWith("PageBackHandler"))
@Composable
fun TaskFlowPageBackHandler(
    onNavigateUp: () -> Unit,
    onBackIntercept: (() -> Boolean)? = null,
    enabled: Boolean = true,
) = PageBackHandler(onNavigateUp, onBackIntercept, enabled)

@Deprecated(INFRA_DEPRECATION, ReplaceWith("TabRootBackHandler"))
@Composable
fun TaskFlowTabRootBackHandler(
    enabled: Boolean = true,
    onBack: (() -> Unit)? = null,
) = TabRootBackHandler(enabled, onBack)

@Deprecated(INFRA_DEPRECATION, ReplaceWith("ImmersiveStatusBarEffect"))
@Composable
fun TaskFlowImmersiveStatusBarEffect(
    enabled: Boolean,
    useDarkStatusBarIcons: Boolean? = null,
) = ImmersiveStatusBarEffect(enabled, useDarkStatusBarIcons)

@Deprecated(INFRA_DEPRECATION, ReplaceWith("NoRippleContent"))
@Composable
fun TaskFlowNoRippleContent(
    content: @Composable () -> Unit,
) = NoRippleContent(content)

@Deprecated(INFRA_DEPRECATION, ReplaceWith("imePadding"))
fun Modifier.taskFlowImePadding(state: ImePaddingState): Modifier = imePadding(state)

@Deprecated(INFRA_DEPRECATION, ReplaceWith("ConfirmDialog"))
@Composable
fun TaskFlowConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String? = null,
    dismissText: String? = null,
) = ConfirmDialog(title, message, onConfirm, onDismiss, confirmText, dismissText)

@Deprecated(INFRA_DEPRECATION, ReplaceWith("ConfirmDialog"))
@Composable
fun TaskFlowConfirmDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String? = null,
    dismissText: String? = null,
    content: @Composable () -> Unit,
) = ConfirmDialog(title, onConfirm, onDismiss, confirmText, dismissText, content)

@Deprecated(INFRA_DEPRECATION, ReplaceWith("BottomSheet"))
@Composable
fun TaskFlowBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = BottomSheet(onDismissRequest, modifier, content)
