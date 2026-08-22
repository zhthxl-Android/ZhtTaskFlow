package com.example.zhttaskflow.feature.article.navigation

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.zhttaskflow.core.network.TaskFlowWanAndroidApiConfig
import com.example.zhttaskflow.feature.article.data.ArticleDataConfig
import com.example.zhttaskflow.feature.article.data.ArticleRepositoryFactory
import com.example.zhttaskflow.feature.article.domain.ArticleRepository
import com.example.zhttaskflow.feature.article.domain.usecase.GetArticlePageUseCase
import com.example.zhttaskflow.feature.article.domain.usecase.RefreshArticlePageUseCase
import com.example.zhttaskflow.feature.article.presentation.ArticleDetailScreen
import com.example.zhttaskflow.feature.article.presentation.ArticleListScreen
import com.example.zhttaskflow.feature.article.presentation.ArticleUiEffect
import com.example.zhttaskflow.feature.article.presentation.ArticleViewModel
import com.example.zhttaskflow.feature.article.presentation.ArticleViewModelFactory
import com.example.zhttaskflow.nav.LocalTaskFlowNavigator
import com.example.zhttaskflow.nav.TaskFlowNavigator
import com.example.zhttaskflow.nav.route.TaskFlowArticleNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowRoute
import com.example.zhttaskflow.nav.route.TaskFlowRouteRegistry
import com.example.zhttaskflow.nav.route.simpleRouteEntry
import com.example.zhttaskflow.nav.route.twoStringArgsRouteEntry

/**
 * 资讯模块路由注册入口（路由常量统一引用 [TaskFlowArticleNavRoutes]）。
 */
sealed interface ArticleRoute : TaskFlowRoute {

    data object List : ArticleRoute {
        override val route: String = TaskFlowArticleNavRoutes.ARTICLE_LIST
    }
}

/**
 * 注册资讯列表与详情路由（在 app / standalone NavHost 中调用）。
 */
fun registerArticleRoutes(
    registry: TaskFlowRouteRegistry,
    @Suppress("UNUSED_PARAMETER") navigator: TaskFlowNavigator,
    repository: ArticleRepository? = null,
    articleDataConfig: ArticleDataConfig? = null,
    useMockRemote: Boolean = false,
) {
    registry.register(
        simpleRouteEntry(
            route = TaskFlowArticleNavRoutes.ARTICLE_LIST,
            content = {
                ArticleListRouteHost(
                    repository = repository,
                    articleDataConfig = articleDataConfig,
                    useMockRemote = useMockRemote,
                )
            },
        ),
    )
    registry.register(
        twoStringArgsRouteEntry(
            route = TaskFlowArticleNavRoutes.ARTICLE_DETAIL,
            firstArgumentName = TaskFlowArticleNavRoutes.ARG_ARTICLE_ID,
            secondArgumentName = TaskFlowArticleNavRoutes.ARG_DETAIL_URL,
            content = { articleId, detailUrl ->
                val navigator = LocalTaskFlowNavigator.current
                ArticleDetailScreen(
                    articleId = Uri.decode(articleId),
                    detailUrl = Uri.decode(detailUrl),
                    onNavigateUp = { navigator.navigateUp() },
                )
            },
        ),
    )
}

@Composable
private fun ArticleListRouteHost(
    repository: ArticleRepository?,
    articleDataConfig: ArticleDataConfig?,
    useMockRemote: Boolean,
) {
    val context = LocalContext.current
    val navigator = LocalTaskFlowNavigator.current
    val factory = rememberArticleViewModelFactory(
        repository = repository,
        articleDataConfig = articleDataConfig,
        useMockRemote = useMockRemote,
    )
    val viewModel: ArticleViewModel = viewModel(factory = factory)

    LaunchedEffect(viewModel) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ArticleUiEffect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ArticleUiEffect.NavigateToDetail -> {
                    navigator.navigate(effect.url)
                }
            }
        }
    }

    ArticleListScreen(viewModel = viewModel)
}

/**
 * 依赖组装层：配置兜底 → Repository → UseCase → [ArticleViewModelFactory]。
 *
 * 不包含业务逻辑；[remember] 缓存 key 与生命周期与原 [ArticleListRouteHost] 内联实现一致。
 */
@Composable
private fun rememberArticleViewModelFactory(
    repository: ArticleRepository?,
    articleDataConfig: ArticleDataConfig?,
    useMockRemote: Boolean,
): ArticleViewModelFactory {
    val context = LocalContext.current
    val resolvedRepository = repository ?: remember(context, articleDataConfig, useMockRemote) {
        val config = articleDataConfig ?: ArticleDataConfig(
            baseUrl = TaskFlowWanAndroidApiConfig.PRODUCTION_BASE_URL,
        )
        ArticleRepositoryFactory.create(
            context = context,
            config = config,
            useMockRemote = useMockRemote,
        )
    }
    return remember(resolvedRepository) {
        val getArticlePageUseCase = GetArticlePageUseCase(resolvedRepository)
        val refreshArticlePageUseCase = RefreshArticlePageUseCase(resolvedRepository)
        ArticleViewModelFactory(
            getArticlePageUseCase = getArticlePageUseCase,
            refreshArticlePageUseCase = refreshArticlePageUseCase,
        )
    }
}
