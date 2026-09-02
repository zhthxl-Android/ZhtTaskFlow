package com.example.zhttaskflow.feature.log.standalone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.zhttaskflow.feature.log.navigation.registerLogRoutes
import com.example.zhttaskflow.nav.rememberTaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowLogNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistryImpl
import com.example.zhttaskflow.nav.standalone.TaskFlowFeatureDebugShell
import com.example.zhttaskflow.nav.standalone.prepareTaskFlowFeatureDebug
import com.example.zhttaskflow.nav.theme.TaskFlowTheme

/**
 * 通过 Gradle 属性 `feature.log.standalone=true` 切换为 application 模块后单独安装运行。
 */
class LogDebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareTaskFlowFeatureDebug()
        setContent {
            TaskFlowTheme {
                val navigator = rememberTaskFlowNavigator()
                val routeRegistry = remember(navigator) {
                    TaskFlowRouteRegistryImpl().also { registry ->
                        registerLogRoutes(
                            registry = registry,
                            navigator = navigator,
                        )
                    }
                }
                TaskFlowFeatureDebugShell(
                    registry = routeRegistry,
                    startDestination = TaskFlowLogNavRoutes.LOG_ROUTE,
                    navigator = navigator,
                    mainTabRootRoute = TaskFlowLogNavRoutes.LOG_ROUTE,
                )
            }
        }
    }
}
