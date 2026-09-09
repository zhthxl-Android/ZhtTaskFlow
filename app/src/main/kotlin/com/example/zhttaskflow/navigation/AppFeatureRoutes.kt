package com.example.zhttaskflow.navigation

import com.example.zhttaskflow.feature.article.navigation.registerArticleRoutes
import com.example.zhttaskflow.feature.log.navigation.registerLogRoutes
import com.example.zhttaskflow.feature.task.navigation.registerTaskRoutes
import com.example.zhttaskflow.nav.AppNavigator
import com.example.zhttaskflow.nav.route.RouteRegistry

/**
 * 壳工程路由装配：按「资讯 → 任务 → 日志」顺序注册各 Feature，不实现任何业务页面。
 */
internal fun RouteRegistry.registerAppRoutes(
    navigator: AppNavigator,
) {
    registerArticleRoutes(registry = this, navigator = navigator)
    registerTaskRoutes(registry = this, navigator = navigator)
    registerLogRoutes(registry = this, navigator = navigator)
}
