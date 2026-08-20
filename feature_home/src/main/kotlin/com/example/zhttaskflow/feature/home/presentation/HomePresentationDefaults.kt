package com.example.zhttaskflow.feature.home.presentation

import android.content.Context
import com.example.zhttaskflow.feature.home.R
import com.example.zhttaskflow.feature.home.domain.HomeEntranceIds

/**
 * 构建首页内置固定入口数据与路由映射（无领域仓库依赖）。
 */
internal object HomePresentationDefaults {

    fun buildFixedHomePageData(context: Context): HomePageData {
        return HomePageData(
            entrances = listOf(
                HomeEntrance(
                    id = HomeEntranceIds.TASK,
                    title = context.getString(R.string.home_str_home_task_title),
                    description = context.getString(R.string.home_str_home_task_desc),
                ),
                HomeEntrance(
                    id = HomeEntranceIds.ARTICLE,
                    title = context.getString(R.string.home_str_home_article_title),
                    description = context.getString(R.string.home_str_home_article_desc),
                ),
            ),
        )
    }

    fun buildEntranceRouteMap(
        taskListRoute: String?,
        articleListRoute: String?,
    ): Map<String, String> {
        return buildMap {
            taskListRoute?.let { put(HomeEntranceIds.TASK, it) }
            articleListRoute?.let { put(HomeEntranceIds.ARTICLE, it) }
        }
    }
}
