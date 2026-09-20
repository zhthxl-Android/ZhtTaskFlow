package com.example.zhttaskflow.base.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.zhttaskflow.base.R
import com.example.zhttaskflow.base.ext.TabRootBackHandler
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.theme.AppColors
import com.example.zhttaskflow.base.ui.skeleton.SkeletonListTemplate
import com.example.zhttaskflow.base.ui.skeleton.SkeletonTemplate
import androidx.compose.material3.pulltorefresh.PullToRefreshBox as MaterialPullToRefreshBox

/**
 * 一级 Tab 根页面脚手架：可选顶栏、FAB、滑动折叠顶栏与沉浸式头部。
 *
 * 这个组件提供了一个灵活的页面结构，支持多种UI元素组合，适用于列表类页面。
 * 它内部委托给 [BaseScaffold] 进行基础布局，并提供了额外的功能支持。
 *
 * 内部委托 [BaseScaffold]：若 App 壳层已提供全局宿主，则自动继承父级 Snackbar/Loading/Dialog，
 * 不再创建第二套宿主；独立调试无外层壳时由 [BaseScaffold] 自动降级为本地宿主。
 *
 * ## 沉浸式顶栏 [immersiveTop]
 * 开启后内容区不再预留状态栏 padding，Banner/轮播可延伸至状态栏下；与 [title]/[actions] 互斥（有顶栏时自动关闭沉浸）。
 * 头部文案/按钮请使用 [rememberStatusBarTopInset] 避开状态栏。
 *
 * @param immersiveStatusBarUseDarkIcons 状态栏图标是否深色；`null` 时随系统主题自动适配
 * @param interceptTabRootBackToDesktop 一级 Tab 根页为 `true` 时，系统返回退到桌面而非退出应用
 * @param onTabRootBackPress 自定义 Tab 根返回；默认 [com.example.zhttaskflow.base.ext.moveTaskToDesktop]
 *
 * ## 列表内容接入（与 [StateBox] 配合）
 *
 * 1. `ListScaffold { scaffoldPadding -> ... }` 内放置 [StateBox]，`contentPadding = rememberStateBoxContentPadding()`。
 * 2. 仅刷新：成功态使用 [StateRefreshableListContent]（`RefreshableListPayload`）。
 * 3. 分页： [StatePaginatedListContent] + [rememberPaginationController]。
 * 4. LazyColumn 的 `contentPadding` 使用 [rememberListLazyContentPadding]（含 Tab 底栏避让）。
 *
 * @see com.example.zhttaskflow.base.doc.BaseArchitecture
 */
@Composable
fun ListScaffold(
    title: String? = null,//栏标题
    modifier: Modifier = Modifier,
    actions: (@Composable RowScope.() -> Unit)? = null,//标题栏右侧操作按钮组，运行在 Row 作用域内，可放置多个图标按钮
    floatingActionButton: @Composable () -> Unit = {},//悬浮按钮（FAB），透传给内层 BaseScaffold
    collapsibleTopBarOnScroll: Boolean = false,//是否开启**滚动折叠顶栏**：列表下滑时顶栏收起，上滑时展开
    immersiveTop: Boolean = false,//是否开启**沉浸式顶部**：内容延伸到状态栏下方，适合 Banner / 轮播场景；**和顶栏互斥**，有 title 时自动失效
    immersiveStatusBarUseDarkIcons: Boolean? = null,//沉浸式系统状态栏图标颜色：true = 深色，false = 浅色，null = 跟随系统主题自动适配
    interceptTabRootBackToDesktop: Boolean = false,//是否拦截 Tab 根页返回键：true 时按返回键将应用退到后台桌面，不退出应用
    onTabRootBackPress: (() -> Unit)? = null,//自定义返回键回调，优先级高于默认的「退到桌面」逻辑
    content: @Composable (PaddingValues) -> Unit,
) {
    //判断是否渲染标题栏，true显示，false 不显示
    val topBarShouldCompose = topBarShouldCompose(
        title = title,
        hasTrailingSlot = actions != null,
    )
    //开启沉浸且无标题栏（沉浸式模式和顶栏是互斥的）
    //只有同时满足「用户开了沉浸式」且「没有顶栏」，沉浸式才真正生效
    //如果有顶栏，就算开了 immersiveTop 也会自动失效，因为顶栏本身就要占位置，无法沉浸
    val immersiveActive = immersiveTop && !topBarShouldCompose
    //是否开启滚动折叠顶标题栏功能
    val collapseEnabled = collapsibleTopBarOnScroll
    //标题栏折叠的时候，要不要把系统状态栏的高度也算进折叠区域里
    //只有折叠功能开启了，且没有开启沉浸式
    val collapseIncludesStatusBar = collapseEnabled && !immersiveActive
    //标题栏自身内容区高度
    val topBarContentHeight = if (topBarShouldCompose) {
        UiConstants.TopBarHeight
    } else {
        0.dp
    }
    //折叠标题栏的状态管理器
    val collapseState = rememberCollapsibleTopBarState(
        enabled = collapseEnabled,//是否启用
        topBarContentHeight = topBarContentHeight,
        includeStatusBarInset = collapseIncludesStatusBar,//是否包含状态栏高度
    )
    //获取当前屏幕的密度
    val density = LocalDensity.current

    //副作用修改状态栏外观
    ImmersiveStatusBarEffect(
        enabled = immersiveActive,
        useDarkStatusBarIcons = immersiveStatusBarUseDarkIcons,
    )

    //处理 Tab 根页返回逻辑
    TabRootBackHandler(
        enabled = interceptTabRootBackToDesktop,
        onBack = onTabRootBackPress,
    )

    BaseScaffold(
        modifier = modifier,
        //只有既没有标题栏、又不折叠、又不沉浸的时候，才让内容区自动加状态栏内边距，否则由标题栏 / 折叠区域自己处理
        consumeStatusBarsInContent = !topBarShouldCompose && !collapseEnabled && !immersiveActive,
        floatingActionButton = floatingActionButton,
        contentModifier = if (collapseEnabled) {
            //列表的滚动事件和折叠状态管理器连接起来
            Modifier.nestedScroll(collapseState.nestedScrollConnection)
        } else {
            Modifier
        },
        //标题栏
        header = {
            //开启滚动折叠
            if (collapseEnabled) {
                val visibleHeightPx = collapseState.visibleHeaderHeightPx()
                //当前可见的头部高度，随折叠进度实时变化
                val visibleHeight = with(density) { visibleHeightPx.toDp() }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(visibleHeight)
                        .clipToBounds(),//裁剪超出 Box 范围的内容
                ) {
                    Column(
                        modifier = Modifier.offset {
                            IntOffset(
                                0,
                                collapseState.headerOffsetPx()
                            )
                        },
                    ) {
                        if (topBarShouldCompose) {
                            TopBar(
                                title = title,
                                trailing = actions,
                                applyStatusBarsPadding = true,
                                collapseProgress = 1f - collapseState.collapseProgress,
                            )
                        } else if (collapseIncludesStatusBar) {
                            //如果没有顶栏但折叠包含状态栏
                            Spacer(modifier = Modifier.statusBarsPadding())
                        }
                    }
                }
            } else if (topBarShouldCompose) {
                //没有折叠功能
                TopBar(
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
data class ListPaginationState(
    val isRefreshing: Boolean = false,//是否正在下拉刷新
    val isLoadingMore: Boolean = false,//是否正在上拉加载更多
    val isLoadMoreError: Boolean = false,//加载更多是否失败
    val hasMore: Boolean = true,//是否还有更多数据，没有就显示「没有更多了」
)

/**
 * 分页列表
 * 与 [StateBox] 配合的成功态列表载荷：业务列表 + 分页状态。
 */
data class PaginatedListPayload<T>(
    val items: List<T>,
    val pagination: ListPaginationState,
)

/**
 * 普通刷新列表
 * 与 [StateBox] 配合的成功态列表载荷：业务列表 + 下拉刷新中状态（非分页场景）。
 */
data class RefreshableListPayload<T>(
    val items: List<T>,
    val isRefreshing: Boolean = false,
)

/**
 * 分页控制器
 * 分页页码辅助：ViewModel 在刷新/加载更多成功后调用 [applyPageResult] 更新页码与 hasMore。
 */
@Stable
class PaginationController(
    initialPage: Int = 0,
) {
    //当前已加载的最新页码
    var currentPage by mutableIntStateOf(initialPage)
        private set

    //是否还有下一页
    var hasMore by mutableStateOf(true)
        private set

    /** 下拉刷新应请求的页码（从 1 开始）。 */
    fun pageForRefresh(): Int = 1

    /** 上拉加载更多应请求的页码。 */
    fun pageForLoadMore(): Int = if (currentPage <= 0) 1 else currentPage + 1

    //接口请求成功后调用，更新当前页码和是否还有更多数据
    fun applyPageResult(
        page: Int,
        hasMore: Boolean
    ) {
        currentPage = page
        this.hasMore = hasMore
    }

    //重置页码和状态，用于重新加载、筛选切换场景
    fun reset() {
        currentPage = 0
        hasMore = true
    }
}

//创建分页控制器实例
@Composable
fun rememberPaginationController(initialPage: Int = 0): PaginationController =
    remember { PaginationController(initialPage = initialPage) }

/**
 * 全局统一样式的下拉刷新容器。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val refreshState = rememberPullToRefreshState()
    val indicatorColor = AppColors.primary
    val indicatorContainerColor = MaterialTheme.colorScheme.surface
    MaterialPullToRefreshBox(
        isRefreshing = isRefreshing,//刷新状态，由外部控制，true 时显示刷新指示器
        onRefresh = onRefresh,//下拉释放时触发的刷新回调
        state = refreshState,//创建并缓存官方下拉刷新状态，处理拖动手势和动画
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
fun ListLoadMoreFooter(
    pagination: ListPaginationState,
    onRetryLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(UiConstants.ListLoadMoreFooterMinHeight),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !pagination.hasMore -> {
                Text(
                    text = stringResource(id = R.string.base_str_list_no_more),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.onSurfaceVariant,
                )
            }

            pagination.isLoadingMore -> {
                CircularProgressIndicator(
                    modifier = Modifier.height(UiConstants.ListLoadMoreProgressHeight),
                    color = AppColors.primary,
                    strokeWidth = UiConstants.ListLoadMoreProgressStrokeWidth,
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
                    color = AppColors.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 *
 * 成功状态下，分页列表组件整合下拉刷新、懒加载列表、自动预加载、分页尾部
 * 带下拉刷新与上拉加载更多的 [LazyColumn] 列表（不含首屏四态，成功态数据展示用）。
 */
@Composable
fun <T> PaginatedList(
    items: List<T>,//业务数据列表
    pagination: ListPaginationState,//分页状态
    onRefresh: () -> Unit,//下拉刷新回调
    onLoadMore: () -> Unit,//加载更多回调，滚动到底部附近自动触发
    listContentPadding: PaddingValues,//LazyColumn 的内边距，用来避让顶栏、底栏、Tab 栏
    modifier: Modifier = Modifier,
    onRetryLoadMore: () -> Unit = onLoadMore,//加载失败重试回调，默认和加载更多共用
    loadMorePrefetchThreshold: Int = UiConstants.ListLoadMorePrefetchThreshold,//预加载阈值：距离最后一条还有多少项时触发加载
    key: ((index: Int, item: T) -> Any)? = null,//条目唯一键生成函数，提升列表滑动性能
    itemContent: @Composable (index: Int, item: T) -> Unit,//单条目的 UI 渲染 lambda
) {
    //懒加载列表状态，用于滚动监听和分页加载
    val listState = rememberLazyListState()

    LaunchedEffect(
        listState,
        pagination.hasMore,
        pagination.isLoadingMore,
        pagination.isLoadMoreError
    ) {
        //把列表状态转成 Flow 流，每次滚动都会发射新的值
        snapshotFlow {
            //获取当前列表的布局信息
            val info = listState.layoutInfo
            //当前屏幕上最后一个可见条目的索引
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            //发射「最后可见索引 + 总条目数」的配对数据
            lastVisible to info.totalItemsCount
        }.collect { (lastVisible, total) ->
            if (
                pagination.hasMore &&//还有更多数据
                !pagination.isLoadingMore &&//当前没有在加载更多
                !pagination.isLoadMoreError &&//当前没有加载更多失败
                total > 0 &&//总条目数大于 0
                lastVisible >= total - loadMorePrefetchThreshold//最后可见项距离底部小于阈值，触发预加载
            ) {
                onLoadMore()
            }
        }
    }

    val listModifier = Modifier.fillMaxSize()
    //当列表为空且正在下拉刷新时
    if (items.isEmpty() && pagination.isRefreshing) {
        //只显示刷新指示器，不渲染空列表
        PullToRefreshBox(
            isRefreshing = true,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize(),
        ) {
            Box(modifier = listModifier)
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = pagination.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,//绑定之前创建的`listState`，和预加载逻辑联动
            modifier = listModifier,
            verticalArrangement = Arrangement.spacedBy(UiConstants.ListVerticalSpacing),
            contentPadding = listContentPadding,
        ) {
            if (key != null) {
                itemsIndexed(
                    items = items,
                    key = { index, item ->
                        key(
                            index,
                            item
                        )
                    },
                ) { index, item ->
                    itemContent(
                        index,
                        item
                    )
                }
            } else {
                itemsIndexed(items = items) { index, item ->
                    itemContent(
                        index,
                        item
                    )
                }
            }
            //固定加一个尾部条目
            item(key = "taskflow_load_more_footer") {
                //渲染`ListLoadMoreFooter`分页尾部组件
                ListLoadMoreFooter(
                    pagination = pagination,
                    onRetryLoadMore = onRetryLoadMore,
                )
            }
        }
    }
}

/**
 * 非分页场景的列表组件，只有下拉刷新，没有加载更多和分页尾部
 * 带下拉刷新的 [LazyColumn] 列表（不含首屏四态与分页尾部，成功态数据展示用）。
 */
@Composable
fun <T> RefreshableList(
    items: List<T>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    listContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    key: ((index: Int, item: T) -> Any)? = null,
    itemContent: @Composable (index: Int, item: T) -> Unit,
) {
    val listModifier = Modifier.fillMaxSize()
    if (items.isEmpty() && isRefreshing) {
        PullToRefreshBox(
            isRefreshing = true,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize(),
        ) {
            Box(modifier = listModifier)
        }
        return
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            modifier = listModifier,
            verticalArrangement = Arrangement.spacedBy(UiConstants.ListVerticalSpacing),
            contentPadding = listContentPadding,
        ) {
            if (key != null) {
                itemsIndexed(
                    items = items,
                    key = { index, item ->
                        key(
                            index,
                            item
                        )
                    },
                ) { index, item ->
                    itemContent(
                        index,
                        item
                    )
                }
            } else {
                itemsIndexed(items = items) { index, item ->
                    itemContent(
                        index,
                        item
                    )
                }
            }
        }
    }
}

/**
 * 首屏加载时的列表骨架屏
 * 列表脚手架默认首屏加载骨架（与 [StatePaginatedListContent]、[StateRefreshableListContent] 默认 loading 一致）。
 */
@Composable
fun ListSkeletonLoading(
    listContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    itemCount: Int = UiConstants.SkeletonDefaultListItemCount,
    template: SkeletonTemplate = SkeletonTemplate.List,
) {
    SkeletonListTemplate(
        contentPadding = listContentPadding,
        modifier = modifier.fillMaxSize(),
        itemCount = itemCount,
        template = template,
    )
}

/**
 * 提前绑定骨架屏的所有参数，返回一个符合 StateBox 要求的 lambda
 * 供 [StateBox] 使用的列表骨架 loading 工厂（可交给 [StateBox] 的 `loading` 参数）。
 */
@Composable
fun rememberListSkeletonLoading(
    listContentPadding: PaddingValues,
    itemCount: Int = UiConstants.SkeletonDefaultListItemCount,
    template: SkeletonTemplate = SkeletonTemplate.List,
): @Composable (Modifier) -> Unit {
    return remember(
        listContentPadding,
        itemCount,
        template
    ) {
        { loadingModifier ->
            ListSkeletonLoading(
                listContentPadding = listContentPadding,
                modifier = loadingModifier,
                itemCount = itemCount,
                template = template,
            )
        }
    }
}

/**
 * 一站式组件: 四态切换 + 下拉刷新 + 分页加载 + 骨架屏全部封装好了
 * [StateBox] + 下拉刷新 + 分页列表一站式封装：首屏 Loading/Empty/Error 与成功态列表统一处理。
 *
 * 业务只需提供 [uiState]、刷新/重试/加载更多回调与 [itemContent]。
 */
@Composable
fun <T> StatePaginatedListContent(
    uiState: BaseUiState<PaginatedListPayload<T>>,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    listContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onRetryLoadMore: () -> Unit = onLoadMore,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = UiConstants.PageHorizontalPadding,
    ),
    emptyMessage: String = stringResource(id = R.string.base_str_empty),
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
) {
    //四态容器
    StateBox(
        uiState = uiState,
        onRetry = onRetry,
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize(),
        emptyMessage = emptyMessage,
        loading = loading,
    ) { payload ->
        //Success 态下,渲染分页列表
        PaginatedList(
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

/**
 * 非分页场景的一站式组件
 * [StateBox] + 下拉刷新 + 非分页列表一站式封装：首屏 Loading/Empty/Error 与成功态列表统一处理。
 *
 * API 与 [StatePaginatedListContent] 对齐（无加载更多与分页尾部）。
 */
@Composable
fun <T> StateRefreshableListContent(
    uiState: BaseUiState<RefreshableListPayload<T>>,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    listContentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = UiConstants.PageHorizontalPadding,
    ),
    emptyMessage: String = stringResource(id = R.string.base_str_empty),
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
) {
    StateBox(
        uiState = uiState,
        onRetry = onRetry,
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize(),
        emptyMessage = emptyMessage,
        loading = loading,
    ) { payload ->
        RefreshableList(
            items = payload.items,
            isRefreshing = payload.isRefreshing,
            onRefresh = onRefresh,
            listContentPadding = listContentPadding,
            key = key,
            itemContent = itemContent,
        )
    }
}

