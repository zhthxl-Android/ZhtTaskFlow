package com.example.zhttaskflow.feature.home.presentation

import com.example.zhttaskflow.base.mvi.BaseUiState

/**
 * 首页单个功能入口展示数据（Success 态列表项）。
 *
 * @param id 入口唯一标识，与 [com.example.zhttaskflow.feature.home.domain.HomeEntranceIds] 对齐
 * @param title 功能名称
 * @param description 功能描述
 */
data class HomeEntrance(
    val id: String,
    val title: String,
    val description: String,
)

/**
 * 首页业务载荷（仅出现在 [BaseUiState.Success] 中）。
 *
 * @param entrances 功能入口列表
 * @param banner 预留：顶部 Banner 数据
 * @param announcement 预留：公告/运营位数据
 */
data class HomePageData(
    val entrances: List<HomeEntrance>,
    val banner: String? = null,
    val announcement: String? = null,
)

/** 首页 UI 状态：`BaseUiState` 通用分支 + [HomePageData] 业务数据。 */
typealias HomeUiState = BaseUiState<HomePageData>
