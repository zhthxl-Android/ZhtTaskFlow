package com.example.zhttaskflow.feature.task.standalone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.zhttaskflow.feature.task.navigation.TaskRoute
import com.example.zhttaskflow.feature.task.navigation.registerTaskRoutes
import com.example.zhttaskflow.nav.TaskFlowNavHost
import com.example.zhttaskflow.nav.rememberTaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistryImpl
import com.example.zhttaskflow.nav.theme.TaskFlowTheme

/** Feature 独立调试入口：与宿主一致使用 [TaskFlowNavHost]。 */
class FeatureTaskDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskFlowTheme {
                val navigator = rememberTaskFlowNavigator()
                val routeRegistry = remember(navigator) {
                    TaskFlowRouteRegistryImpl().also { registry ->
                        registerTaskRoutes(registry, navigator)
                    }
                }
                TaskFlowNavHost(
                    registry = routeRegistry,
                    startDestination = TaskRoute.ROUTE_LIST,
                    navigator = navigator,
                )
            }
        }
    }
}
