package com.example.zhttaskflow.feature.home.standalone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.zhttaskflow.feature.home.navigation.registerHomeRoutes
import com.example.zhttaskflow.nav.rememberTaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowHomeNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistryImpl
import com.example.zhttaskflow.nav.standalone.TaskFlowFeatureDebugShell
import com.example.zhttaskflow.nav.standalone.prepareTaskFlowFeatureDebug
import com.example.zhttaskflow.nav.theme.TaskFlowTheme

/**
 * 首页 Feature 独立调试入口：嵌套调试壳 + NavHost，边距与正式 [com.example.zhttaskflow.navigation.AppMainShell] 一致。
 *
 * 通过 Gradle 属性 `feature.home.standalone=true` 切换为 application 模块后单独安装运行。
 */
class HomeDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareTaskFlowFeatureDebug()
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
                TaskFlowFeatureDebugShell(
                    registry = routeRegistry,
                    startDestination = TaskFlowHomeNavRoutes.HOME_ROUTE,
                    navigator = navigator,
                    mainTabRootRoute = TaskFlowHomeNavRoutes.HOME_ROUTE,
                )
            }
        }
    }
}
