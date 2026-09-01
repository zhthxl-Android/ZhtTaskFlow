package com.example.zhttaskflow.feature.article.presentation

import com.example.zhttaskflow.base.ext.SnackbarType
import com.example.zhttaskflow.base.mvi.BaseUiEffect

/**
 * 资讯列表页一次性副作用，不写入 [ArticleUiState]。
 *
 * **消费分层**：
 * - 页面内 UI 反馈（如 [ShowSnackbar]）：由 [ArticleListScreen] 消费
 * - 跨页面导航（[NavigateToDetail]）：由 [com.example.zhttaskflow.feature.article.navigation.ArticleListRouteHost] 消费
 */
sealed interface ArticleUiEffect : BaseUiEffect {

    /**
     * 展示全局 Snackbar（页面内 UI 反馈）。
     *
     * @param message 用户可读文案
     * @param type 成功 / 错误 / 普通样式，由业务层指定
     */
    data class ShowSnackbar(
        val message: String,
        val type: SnackbarType = SnackbarType.Normal,
    ) : ArticleUiEffect

    /**
     * @deprecated 请改用 [ShowSnackbar]，并显式传入 [SnackbarType]。
     */
    @Deprecated(
        message = "已更名为 ShowSnackbar，请指定 type 参数",
        replaceWith = ReplaceWith(
            expression = "ShowSnackbar(message, SnackbarType.Normal)",
            imports = [
                "com.example.zhttaskflow.feature.article.presentation.ArticleUiEffect.ShowSnackbar",
                "com.example.zhttaskflow.base.ext.SnackbarType",
            ],
        ),
    )
    data class ShowToast(val message: String) : ArticleUiEffect

    /**
     * 跳转文章详情页（跨页面导航）。
     *
     * @param url 完整 Navigation 路由 path（由 [com.example.zhttaskflow.nav.route.TaskFlowArticleNavRoutes.detailPath] 生成）
     */
    data class NavigateToDetail(val url: String) : ArticleUiEffect
}
