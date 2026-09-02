package com.example.zhttaskflow.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.zhttaskflow.R
import com.example.zhttaskflow.base.ui.icon.TaskFlowIcons
import com.example.zhttaskflow.nav.route.TaskFlowArticleNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowLogNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowTaskNavRoutes

/**
 * 主界面底部 Tab 配置：路由、选中/未选中图标、文案资源。
 */
enum class MainTab(
    val route: String,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Log(
        route = TaskFlowLogNavRoutes.LOG_ROUTE,
        labelResId = R.string.app_tab_log,
        selectedIcon = TaskFlowIcons.Tab.HomeSelected,
        unselectedIcon = TaskFlowIcons.Tab.HomeUnselected,
    ),
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
    );

    companion object {
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
