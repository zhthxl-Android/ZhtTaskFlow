package com.example.zhttaskflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.zhttaskflow.feature.home.navigation.registerHomeRoutes
import com.example.zhttaskflow.navigation.registerAppFeatureRoutes
import com.example.zhttaskflow.nav.TaskFlowNavHost
import com.example.zhttaskflow.nav.rememberTaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowArticleNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistryImpl
import com.example.zhttaskflow.nav.theme.TaskFlowTheme
import com.example.zhttaskflow.feature.task.navigation.TaskRoute

/**
 * 壳 Activity：装配首页与 Feature 路由，[TaskFlowNavHost] 统一导航。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskFlowTheme {
                val navigator = rememberTaskFlowNavigator()
                val routeRegistry = remember(navigator) {
                    TaskFlowRouteRegistryImpl().also { registry ->
                        registerHomeRoutes(
                            registry = registry,
                            navigator = navigator,
                            taskListRoute = TaskRoute.ROUTE_LIST,
                            articleListRoute = TaskFlowArticleNavRoutes.LIST,
                            onHomeBackPress = { finish() },
                        )
                        registry.registerAppFeatureRoutes(navigator = navigator)
                    }
                }
                TaskFlowNavHost(
                    registry = routeRegistry,
                    startDestination = TaskFlowNavRoutes.HOME_ROUTE,
                    navigator = navigator,
                )
            }
        }
    }
}
