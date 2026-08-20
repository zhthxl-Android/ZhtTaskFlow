package com.example.zhttaskflow.feature.article.presentation

import com.example.zhttaskflow.base.mvi.BaseUiEvent

/**
 * 资讯列表用户事件（唯一 UI 输入）。
 */
sealed interface ArticleUiEvent : BaseUiEvent {

    /** 下拉刷新：清空内存缓存并加载第一页 */
    data object Refresh : ArticleUiEvent

    /** 加载下一页 */
    data object LoadMore : ArticleUiEvent

    /** 点击列表项进入详情 */
    data class ArticleClicked(
        val articleId: String,
        val detailUrl: String,
    ) : ArticleUiEvent
}
