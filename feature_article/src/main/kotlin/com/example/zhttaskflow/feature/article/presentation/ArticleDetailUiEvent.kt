package com.example.zhttaskflow.feature.article.presentation

import com.example.zhttaskflow.base.mvi.BaseUiEvent

/**
 * 资讯详情页用户事件。
 */
sealed interface ArticleDetailUiEvent : BaseUiEvent {

    /** 首次进入或路由参数变化时加载详情 */
    data class Load(
        val articleId: String,
        val detailUrl: String,
    ) : ArticleDetailUiEvent

    /** StateBox 错误态重试 */
    data object Retry : ArticleDetailUiEvent
}
