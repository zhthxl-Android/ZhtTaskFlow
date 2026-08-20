package com.example.zhttaskflow.feature.home.standalone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.zhttaskflow.feature.home.navigation.registerHomeRoutes
import com.example.zhttaskflow.nav.TaskFlowNavHost
import com.example.zhttaskflow.nav.rememberTaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistryImpl
import com.example.zhttaskflow.nav.theme.TaskFlowTheme

/** Feature 独立调试入口：仅装配首页路由，与 task/article standalone 模式一致。 */
class FeatureHomeDebugActivity : ComponentActivity() {
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
                            onHomeBackPress = { finish() },
                        )
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
