package com.example.zhttaskflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.zhttaskflow.navigation.registerAppRoutes
import com.example.zhttaskflow.nav.TaskFlowNavHost
import com.example.zhttaskflow.nav.rememberTaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistryImpl
import com.example.zhttaskflow.nav.theme.TaskFlowTheme

/**
 * 壳 Activity：仅负责模块路由装配与 [TaskFlowNavHost] 宿主，不承载业务 UI。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskFlowTheme {
                val navigator = rememberTaskFlowNavigator()
                val routeRegistry = remember(navigator) {
                    TaskFlowRouteRegistryImpl().also { registry ->
                        registry.registerAppRoutes(
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
