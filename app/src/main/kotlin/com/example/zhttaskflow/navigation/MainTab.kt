package com.example.zhttaskflow.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.zhttaskflow.R
import com.example.zhttaskflow.base.ui.icon.AppIcons
import com.example.zhttaskflow.nav.route.ArticleNavRoutes
import com.example.zhttaskflow.nav.route.LogNavRoutes
import com.example.zhttaskflow.nav.route.TaskNavRoutes

/**
 * 主界面底部 Tab 配置：顺序为资讯 → 任务 → 日志；路由、图标与文案资源。
 */
enum class MainTab(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Article(
        route = ArticleNavRoutes.ARTICLE_LIST,
        labelResId = R.string.app_tab_article,
        selectedIcon = AppIcons.Tab.ArticleSelected,
        unselectedIcon = AppIcons.Tab.ArticleUnselected,
    ),
    Task(
        route = TaskNavRoutes.TASK_LIST,
        labelResId = R.string.app_tab_task,
        selectedIcon = AppIcons.Tab.TaskSelected,
        unselectedIcon = AppIcons.Tab.TaskUnselected,
    ),
    Log(
        route = LogNavRoutes.LOG_ROUTE,
        labelResId = R.string.app_tab_log,
        selectedIcon = AppIcons.Tab.LogSelected,
        unselectedIcon = AppIcons.Tab.LogUnselected,
    );

    companion object {
        /** 应用默认启动 Tab（资讯列表）。 */
        val startDestinationRoute: String = Article.route

        /**
         * 根据当前 destination 路由解析 Tab；详情等非 Tab 路由返回 null。
         */
        fun fromRoute(route: String?): MainTab? {
            if (route == null) {
                return null
            }
            return entries.firstOrNull { tab -> tab.route == route }
        }
    }
}

@Deprecated("将在下个版本移除，请使用 MainTab", ReplaceWith("MainTab"))
typealias TaskFlowMainTab = MainTab
