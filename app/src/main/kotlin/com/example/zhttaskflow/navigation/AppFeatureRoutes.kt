package com.example.zhttaskflow.navigation

import com.example.zhttaskflow.feature.article.navigation.registerArticleRoutes
import com.example.zhttaskflow.feature.home.navigation.registerHomeRoutes
import com.example.zhttaskflow.feature.task.navigation.registerTaskRoutes
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowArticleNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowTaskNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry

/**
 * 壳工程路由装配：按「首页 → 任务 → 资讯」顺序注册各 Feature，不实现任何业务页面。
 */
internal fun TaskFlowRouteRegistry.registerAppRoutes(
    navigator: TaskFlowNavigator,
    onHomeBackPress: () -> Unit,
) {
    registerHomeRoutes(
        registry = this,
        navigator = navigator,
        taskListRoute = TaskFlowTaskNavRoutes.TASK_LIST,
        articleListRoute = TaskFlowArticleNavRoutes.ARTICLE_LIST,
        onHomeBackPress = onHomeBackPress,
    )
    registerTaskRoutes(registry = this, navigator = navigator)
    registerArticleRoutes(registry = this, navigator = navigator)
}
