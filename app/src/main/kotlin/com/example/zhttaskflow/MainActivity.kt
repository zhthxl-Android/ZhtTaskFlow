package com.example.zhttaskflow

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

/**
 * 壳 Activity：通过 [TaskFlowNavHost] 装配 Feature 注册的路由。
 *
 * 是否集成 feature_task 由 app 模块 Gradle 依赖控制（feature.task.standalone）；编译进宿主时自动注册任务路由。
 */
class MainActivity : ComponentActivity() {
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
