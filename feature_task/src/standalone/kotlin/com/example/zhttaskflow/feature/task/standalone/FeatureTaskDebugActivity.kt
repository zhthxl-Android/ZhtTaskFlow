package com.example.zhttaskflow.feature.task.standalone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.zhttaskflow.feature.task.navigation.registerTaskRoutes
import com.example.zhttaskflow.nav.rememberTaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistryImpl
import com.example.zhttaskflow.nav.route.TaskFlowTaskNavRoutes
import com.example.zhttaskflow.nav.standalone.TaskFlowFeatureDebugDeepLinkState
import com.example.zhttaskflow.nav.standalone.TaskFlowFeatureDebugShell
import com.example.zhttaskflow.nav.standalone.prepareTaskFlowFeatureDebug
import com.example.zhttaskflow.nav.theme.TaskFlowTheme

/** Feature 独立调试入口：调试壳对齐集成宿主（拦截链、深链、全局 Snackbar）。 */
class FeatureTaskDebugActivity : ComponentActivity() {

    private val deepLinkState = TaskFlowFeatureDebugDeepLinkState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareTaskFlowFeatureDebug()
        deepLinkState.updateFromIntent(intent)
        setContent {
            TaskFlowTheme {
                val navigator = rememberTaskFlowNavigator()
                val routeRegistry = remember(navigator) {
                    TaskFlowRouteRegistryImpl().also { registry ->
                        registerTaskRoutes(registry, navigator)
                    }
                }
                TaskFlowFeatureDebugShell(
                    registry = routeRegistry,
                    startDestination = TaskFlowTaskNavRoutes.TASK_LIST,
                    navigator = navigator,
                    mainTabRootRoute = TaskFlowTaskNavRoutes.TASK_LIST,
                    deepLinkState = deepLinkState,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkState.updateFromIntent(intent)
    }
}
