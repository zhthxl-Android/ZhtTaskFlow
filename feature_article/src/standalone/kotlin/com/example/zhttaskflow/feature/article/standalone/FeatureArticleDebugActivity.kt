package com.example.zhttaskflow.feature.article.standalone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.zhttaskflow.feature.article.navigation.registerArticleRoutes
import com.example.zhttaskflow.nav.TaskFlowNavHost
import com.example.zhttaskflow.nav.rememberTaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowArticleNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistryImpl
import com.example.zhttaskflow.nav.theme.TaskFlowTheme

/** Feature 独立调试入口：使用演示远程数据源，无需真实后端。 */
class FeatureArticleDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskFlowTheme {
                val navigator = rememberTaskFlowNavigator()
                val routeRegistry = remember(navigator) {
                    TaskFlowRouteRegistryImpl().also { registry ->
                        registerArticleRoutes(
                            registry = registry,
                            navigator = navigator,
                            useMockRemote = true,
                        )
                    }
                }
                TaskFlowNavHost(
                    registry = routeRegistry,
                    startDestination = TaskFlowArticleNavRoutes.LIST,
                    navigator = navigator,
                )
            }
        }
    }
}
