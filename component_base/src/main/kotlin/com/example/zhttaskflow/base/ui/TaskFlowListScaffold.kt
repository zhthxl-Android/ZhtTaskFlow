package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.zhttaskflow.base.ext.TaskFlowTabRootBackHandler
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.zhttaskflow.base.R
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.theme.TaskFlowColors
import com.example.zhttaskflow.base.ui.state.BaseLoadingScreen

/**
 * 一级 Tab 根页面脚手架：可选顶栏、FAB、滑动折叠顶栏与沉浸式头部。
 *
 * ## 沉浸式顶栏 [immersiveTop]
 * 开启后内容区不再预留状态栏 padding，Banner/轮播可延伸至状态栏下；与 [title]/[actions] 互斥（有顶栏时自动关闭沉浸）。
 * 头部文案/按钮请使用 [rememberTaskFlowStatusBarTopInset] 避开状态栏。
 *
 * @param immersiveStatusBarUseDarkIcons 状态栏图标是否深色；`null` 时随系统主题自动适配
 * @param interceptTabRootBackToDesktop 一级 Tab 根页为 `true` 时，系统返回退到桌面而非退出应用
 * @param onTabRootBackPress 自定义 Tab 根返回；默认 [com.example.zhttaskflow.base.ext.moveTaskToDesktop]
 */
@Composable
fun TaskFlowListScaffold(
    title: String? = null,
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    collapsibleTopBarOnScroll: Boolean = false,
    immersiveTop: Boolean = false,
    immersiveStatusBarUseDarkIcons: Boolean? = null,
    interceptTabRootBackToDesktop: Boolean = false,
    onTabRootBackPress: (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val topBarShouldCompose = taskFlowTopBarShouldCompose(
        title = title,
        hasTrailingSlot = actions != null,
    )
    val immersiveActive = immersiveTop && !topBarShouldCompose
    val collapseEnabled = collapsibleTopBarOnScroll
    val collapseIncludesStatusBar = collapseEnabled && !immersiveActive
    val topBarContentHeight = if (topBarShouldCompose) {
        TaskFlowUiConstants.PageTitleBarHeight
    } else {
        0.dp
    }
    val collapseState = rememberCollapsibleTopBarState(
        enabled = collapseEnabled,
        topBarContentHeight = topBarContentHeight,
        includeStatusBarInset = collapseIncludesStatusBar,
    )
    val density = LocalDensity.current

    TaskFlowImmersiveStatusBarEffect(
        enabled = immersiveActive,
        useDarkStatusBarIcons = immersiveStatusBarUseDarkIcons,
    )

    TaskFlowTabRootBackHandler(
        enabled = interceptTabRootBackToDesktop,
        onBack = onTabRootBackPress,
    )

    TaskFlowBaseScaffold(
        modifier = modifier,
        consumeStatusBarsInContent = !topBarShouldCompose && !collapseEnabled && !immersiveActive,
        floatingActionButton = floatingActionButton,
        contentModifier = if (collapseEnabled) {
            Modifier.nestedScroll(collapseState.nestedScrollConnection)
        } else {
            Modifier
        },
        header = {
            if (collapseEnabled) {
                val visibleHeightPx = collapseState.visibleHeaderHeightPx()
                val visibleHeight = with(density) { visibleHeightPx.toDp() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(visibleHeight)
                        .clipToBounds(),
                ) {
                    Column(
                        modifier = Modifier.offset {
                            IntOffset(0, collapseState.headerOffsetPx())
                        },
                    ) {
                        if (topBarShouldCompose) {
                            TaskFlowTopBar(
                                title = title,
                                trailing = actions,
                                applyStatusBarsPadding = true,
                                collapseProgress = 1f - collapseState.collapseProgress,
                            )
                        } else if (collapseIncludesStatusBar) {
                            Spacer(modifier = Modifier.statusBarsPadding())
                        }
                    }
                }
            } else if (topBarShouldCompose) {
                TaskFlowTopBar(
                    title = title,
                    trailing = actions,
                    applyStatusBarsPadding = true,
                )
            }
        },
        content = content,
    )
}

/**
 * 列表分页尾部状态（下拉刷新、加载更多、失败重试、没有更多）。
 */
data class TaskFlowListPaginationState(
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoadMoreError: Boolean = false,
    val hasMore: Boolean = true,
)

/**
 * 与 [StateBox] 配合的成功态列表载荷：业务列表 + 分页状态。
 */
data class TaskFlowPaginatedListPayload<T>(
    val items: List<T>,
    val pagination: TaskFlowListPaginationState,
)

/**
 * 分页页码辅助：ViewModel 在刷新/加载更多成功后调用 [applyPageResult] 更新页码与 hasMore。
 */
@Stable
class TaskFlowPaginationController(
    initialPage: Int = 0,
) {
    var currentPage by mutableIntStateOf(initialPage)
        private set

    var hasMore by mutableStateOf(true)
        private set

    /** 下拉刷新应请求的页码（从 1 开始）。 */
    fun pageForRefresh(): Int = 1

    /** 上拉加载更多应请求的页码。 */
    fun pageForLoadMore(): Int = if (currentPage <= 0) 1 else currentPage + 1

    fun applyPageResult(page: Int, hasMore: Boolean) {
        currentPage = page
        this.hasMore = hasMore
    }

    fun reset() {
        currentPage = 0
        hasMore = true
    }
}

@Composable
fun rememberTaskFlowPaginationController(initialPage: Int = 0): TaskFlowPaginationController =
    remember { TaskFlowPaginationController(initialPage = initialPage) }

/**
 * 全局统一样式的下拉刷新容器。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFlowPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val refreshState = rememberPullToRefreshState()
    val indicatorColor = TaskFlowColors.primary
    val indicatorContainerColor = MaterialTheme.colorScheme.surface
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = refreshState,
        modifier = modifier,
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                state = refreshState,
                color = indicatorColor,
                containerColor = indicatorContainerColor,
            )
        },
        content = content,
    )
}

/**
 * 列表底部分页尾部：加载中 / 失败重试 / 没有更多 / 上拉提示。
 */
@Composable
fun TaskFlowListLoadMoreFooter(
    pagination: TaskFlowListPaginationState,
    onRetryLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TaskFlowUiConstants.ListLoadMoreFooterMinHeight),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !pagination.hasMore -> {
                Text(
                    text = stringResource(id = R.string.base_str_list_no_more),
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskFlowColors.onSurfaceVariant,
                )
            }
            pagination.isLoadingMore -> {
                CircularProgressIndicator(
                    modifier = Modifier.height(28.dp),
                    color = TaskFlowColors.primary,
                    strokeWidth = 2.dp,
                )
            }
            pagination.isLoadMoreError -> {
                TextButton(onClick = onRetryLoadMore) {
                    Text(text = stringResource(id = R.string.base_str_list_load_more_retry))
                }
            }
            else -> {
                Text(
                    text = stringResource(id = R.string.base_str_list_load_more_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = TaskFlowColors.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 带下拉刷新与上拉加载更多的 [LazyColumn] 列表（不含首屏四态，成功态数据展示用）。
 */
@Composable
fun <T> TaskFlowPaginatedList(
    items: List<T>,
    pagination: TaskFlowListPaginationState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    listContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onRetryLoadMore: () -> Unit = onLoadMore,
    loadMorePrefetchThreshold: Int = TaskFlowUiConstants.ListLoadMorePrefetchThreshold,
    key: ((index: Int, item: T) -> Any)? = null,
    itemContent: @Composable (index: Int, item: T) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, pagination.hasMore, pagination.isLoadingMore, pagination.isLoadMoreError) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible to info.totalItemsCount
        }.collect { (lastVisible, total) ->
            if (
                pagination.hasMore &&
                !pagination.isLoadingMore &&
                !pagination.isLoadMoreError &&
                total > 0 &&
                lastVisible >= total - loadMorePrefetchThreshold
            ) {
                onLoadMore()
            }
        }
    }

    val listModifier = Modifier.fillMaxSize()
    if (items.isEmpty() && pagination.isRefreshing) {
        TaskFlowPullToRefreshBox(
            isRefreshing = true,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize(),
        ) {
            Box(modifier = listModifier)
        }
        return
    }

    TaskFlowPullToRefreshBox(
        isRefreshing = pagination.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = listModifier,
            verticalArrangement = Arrangement.spacedBy(TaskFlowUiConstants.ListVerticalSpacing),
            contentPadding = listContentPadding,
        ) {
            if (key != null) {
                itemsIndexed(
                    items = items,
                    key = { index, item -> key(index, item) },
                ) { index, item ->
                    itemContent(index, item)
                }
            } else {
                itemsIndexed(items = items) { index, item ->
                    itemContent(index, item)
                }
            }
            item(key = "taskflow_load_more_footer") {
                TaskFlowListLoadMoreFooter(
                    pagination = pagination,
                    onRetryLoadMore = onRetryLoadMore,
                )
            }
        }
    }
}

/**
 * [StateBox] + 下拉刷新 + 分页列表一站式封装：首屏 Loading/Empty/Error 与成功态列表统一处理。
 *
 * 业务只需提供 [uiState]、刷新/重试/加载更多回调与 [itemContent]。
 */
@Composable
fun <T> TaskFlowStatePaginatedListContent(
    uiState: BaseUiState<TaskFlowPaginatedListPayload<T>>,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    listContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onRetryLoadMore: () -> Unit = onLoadMore,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = TaskFlowUiConstants.PageHorizontalPadding,
    ),
    emptyMessage: String = stringResource(id = R.string.base_str_empty),
    loading: @Composable (Modifier) -> Unit = { loadingModifier ->
        BaseLoadingScreen(modifier = loadingModifier)
    },
    key: ((index: Int, item: T) -> Any)? = null,
    itemContent: @Composable (index: Int, item: T) -> Unit,
) {
    StateBox(
        uiState = uiState,
        onRetry = onRetry,
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize(),
        emptyMessage = emptyMessage,
        loading = loading,
    ) { payload ->
        TaskFlowPaginatedList(
            items = payload.items,
            pagination = payload.pagination,
            onRefresh = onRefresh,
            onLoadMore = onLoadMore,
            onRetryLoadMore = onRetryLoadMore,
            listContentPadding = listContentPadding,
            key = key,
            itemContent = itemContent,
        )
    }
}

