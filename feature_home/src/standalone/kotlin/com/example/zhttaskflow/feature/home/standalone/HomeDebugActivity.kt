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

/**
 * 首页 Feature 独立调试入口：嵌套 [TaskFlowNavHost]，仅注册首页路由，不依赖其他业务模块。
 *
 * 通过 Gradle 属性 `feature.home.standalone=true` 切换为 application 模块后单独安装运行。
 */
class HomeDebugActivity : ComponentActivity() {
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
