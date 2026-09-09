package com.example.zhttaskflow.feature.article.standalone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import com.example.zhttaskflow.feature.article.navigation.registerArticleRoutes
import com.example.zhttaskflow.nav.rememberNavigator
import com.example.zhttaskflow.nav.route.ArticleNavRoutes
import com.example.zhttaskflow.nav.route.RouteRegistryImpl
import com.example.zhttaskflow.nav.standalone.FeatureDebugShell
import com.example.zhttaskflow.nav.standalone.prepareFeatureDebug
import com.example.zhttaskflow.nav.theme.AppTheme

/** Feature 独立调试入口：使用演示远程数据源，壳层 inset 与集成宿主一致。 */
class FeatureArticleDebugActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prepareFeatureDebug()
        setContent {
            AppTheme {
                val navigator = rememberNavigator()
                val routeRegistry = remember(navigator) {
                    RouteRegistryImpl().also { registry ->
                        registerArticleRoutes(
                            registry = registry,
                            navigator = navigator,
                            useMockRemote = true,
                        )
                    }
                }
                FeatureDebugShell(
                    registry = routeRegistry,
                    startDestination = ArticleNavRoutes.ARTICLE_LIST,
                    navigator = navigator,
                    mainTabRootRoute = ArticleNavRoutes.ARTICLE_LIST,
                )
            }
        }
    }
}
