package com.example.zhttaskflow.navigation

import com.example.zhttaskflow.feature.article.navigation.registerArticleRoutes
import com.example.zhttaskflow.feature.task.navigation.registerTaskRoutes
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry

/**
 * 壳工程聚合：注册任务与资讯 Feature 路由（首页路由由 [com.example.zhttaskflow.feature.home.navigation.registerHomeRoutes] 注册）。
 */
internal fun TaskFlowRouteRegistry.registerAppFeatureRoutes(navigator: TaskFlowNavigator) {
    registerTaskRoutes(registry = this, navigator = navigator)
    registerArticleRoutes(registry = this, navigator = navigator)
}
