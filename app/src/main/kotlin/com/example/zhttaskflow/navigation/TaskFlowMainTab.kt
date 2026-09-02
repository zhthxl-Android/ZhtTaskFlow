package com.example.zhttaskflow.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.zhttaskflow.R
import com.example.zhttaskflow.base.ui.icon.TaskFlowIcons
import com.example.zhttaskflow.nav.route.TaskFlowArticleNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowLogNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowTaskNavRoutes

/**
 * 主界面底部 Tab 配置：顺序为资讯 → 任务 → 日志；路由、图标与文案资源。
 */
enum class TaskFlowMainTab(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Article(
        route = TaskFlowArticleNavRoutes.ARTICLE_LIST,
        labelResId = R.string.app_tab_article,
        selectedIcon = TaskFlowIcons.Tab.ArticleSelected,
        unselectedIcon = TaskFlowIcons.Tab.ArticleUnselected,
    ),
    Task(
        route = TaskFlowTaskNavRoutes.TASK_LIST,
        labelResId = R.string.app_tab_task,
        selectedIcon = TaskFlowIcons.Tab.TaskSelected,
        unselectedIcon = TaskFlowIcons.Tab.TaskUnselected,
    ),
    Log(
        route = TaskFlowLogNavRoutes.LOG_ROUTE,
        labelResId = R.string.app_tab_log,
        selectedIcon = TaskFlowIcons.Tab.LogSelected,
        unselectedIcon = TaskFlowIcons.Tab.LogUnselected,
    );

    companion object {
        /** 应用默认启动 Tab（资讯列表）。 */
        val startDestinationRoute: String = Article.route

        /**
         * 根据当前 destination 路由解析 Tab；详情等非 Tab 路由返回 null。
         */
        fun fromRoute(route: String?): TaskFlowMainTab? {
            if (route == null) {
                return null
            }
            return entries.firstOrNull { tab -> tab.route == route }
        }
    }
}
