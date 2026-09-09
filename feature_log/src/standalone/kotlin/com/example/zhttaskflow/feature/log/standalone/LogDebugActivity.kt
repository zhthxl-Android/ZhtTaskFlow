package com.example.zhttaskflow.feature.log.standalone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.zhttaskflow.feature.log.navigation.registerLogRoutes
import com.example.zhttaskflow.nav.rememberNavigator
import com.example.zhttaskflow.nav.route.LogNavRoutes
import com.example.zhttaskflow.nav.route.RouteRegistryImpl
import com.example.zhttaskflow.nav.standalone.FeatureDebugShell
import com.example.zhttaskflow.nav.standalone.prepareFeatureDebug
import com.example.zhttaskflow.nav.theme.AppTheme

/**
 * 通过 Gradle 属性 `feature.log.standalone=true` 切换为 application 模块后单独安装运行。
 */
class LogDebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareFeatureDebug()
        setContent {
            AppTheme {
                val navigator = rememberNavigator()
                val routeRegistry = remember(navigator) {
                    RouteRegistryImpl().also { registry ->
                        registerLogRoutes(
                            registry = registry,
                            navigator = navigator,
                        )
                    }
                }
                FeatureDebugShell(
                    registry = routeRegistry,
                    startDestination = LogNavRoutes.LOG_ROUTE,
                    navigator = navigator,
                    mainTabRootRoute = LogNavRoutes.LOG_ROUTE,
                )
            }
        }
    }
}
