package com.example.zhttaskflow.feature.task.standalone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.zhttaskflow.feature.task.navigation.registerTaskRoutes
import com.example.zhttaskflow.nav.rememberNavigator
import com.example.zhttaskflow.nav.route.RouteRegistryImpl
import com.example.zhttaskflow.nav.route.TaskNavRoutes
import com.example.zhttaskflow.nav.standalone.FeatureDebugDeepLinkState
import com.example.zhttaskflow.nav.standalone.FeatureDebugShell
import com.example.zhttaskflow.nav.standalone.prepareFeatureDebug
import com.example.zhttaskflow.nav.theme.AppTheme

/** Feature 独立调试入口：调试壳对齐集成宿主（拦截链、深链、全局 Snackbar）。 */
class FeatureTaskDebugActivity : ComponentActivity() {

    private val deepLinkState = FeatureDebugDeepLinkState()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareFeatureDebug()
        deepLinkState.updateFromIntent(intent)
        setContent {
            AppTheme {
                val navigator = rememberNavigator()
                val routeRegistry = remember(navigator) {
                    RouteRegistryImpl().also { registry ->
                        registerTaskRoutes(registry, navigator)
                    }
                }
                FeatureDebugShell(
                    registry = routeRegistry,
                    startDestination = TaskNavRoutes.TASK_LIST,
                    navigator = navigator,
                    mainTabRootRoute = TaskNavRoutes.TASK_LIST,
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
