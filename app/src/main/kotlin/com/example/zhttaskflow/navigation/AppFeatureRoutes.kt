package com.example.zhttaskflow.navigation

import com.example.zhttaskflow.feature.article.navigation.registerArticleRoutes
import com.example.zhttaskflow.feature.log.navigation.registerLogRoutes
import com.example.zhttaskflow.feature.task.navigation.registerTaskRoutes
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry

/**
 * 壳工程路由装配：按「资讯 → 任务 → 日志」顺序注册各 Feature，不实现任何业务页面。
 */
internal fun TaskFlowRouteRegistry.registerAppRoutes(
    navigator: TaskFlowNavigator,
) {
    registerArticleRoutes(registry = this, navigator = navigator)
    registerTaskRoutes(registry = this, navigator = navigator)
    registerLogRoutes(registry = this, navigator = navigator)
}
