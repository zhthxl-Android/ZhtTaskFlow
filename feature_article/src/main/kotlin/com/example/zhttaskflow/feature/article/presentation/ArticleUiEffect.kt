package com.example.zhttaskflow.feature.article.presentation

import com.example.zhttaskflow.base.mvi.BaseUiEffect

/**
 * 资讯列表页一次性副作用：Toast、导航等由 UI 层消费，不写入 [ArticleUiState]。
 */
sealed interface ArticleUiEffect : BaseUiEffect {

    /**
     * 展示短提示。
     *
     * 触发场景：加载/刷新/加载更多失败，或刷新成功等需要轻提示的场景。
     */
    data class ShowToast(val message: String) : ArticleUiEffect

    /**
     * 跳转文章详情页。
     *
     * @param url 完整 Navigation 路由 path（由 [com.example.zhttaskflow.nav.route.TaskFlowArticleNavRoutes.detailPath] 生成）
     */
    data class NavigateToDetail(val url: String) : ArticleUiEffect
}
