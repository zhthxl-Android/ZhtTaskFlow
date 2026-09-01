package com.example.zhttaskflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.example.zhttaskflow.navigation.AppMainShell
import com.example.zhttaskflow.navigation.registerAppRoutes
import com.example.zhttaskflow.nav.rememberTaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowHomeNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistryImpl
import com.example.zhttaskflow.nav.theme.TaskFlowTheme

/**
 * 壳 Activity：仅负责模块路由装配与 [AppMainShell] 宿主，不承载业务 UI。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskFlowTheme {
                val navigator = rememberTaskFlowNavigator()
                val routeRegistry = remember(navigator) {
                    TaskFlowRouteRegistryImpl().also { registry ->
                        registry.registerAppRoutes(
                            navigator = navigator,
                            onHomeBackPress = { moveTaskToBack(true) },
                        )
                    }
                }
                AppMainShell(
                    registry = routeRegistry,
                    startDestination = TaskFlowHomeNavRoutes.HOME_ROUTE,
                    navigator = navigator,
                )
            }
        }
    }
}
