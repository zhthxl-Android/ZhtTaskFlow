package com.example.zhttaskflow.feature.article.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import com.example.zhttaskflow.base.ext.handleTaskFlowPageBack
import com.example.zhttaskflow.base.extension.collectUiStateWithLifecycle
import com.example.zhttaskflow.base.mvi.BaseUiState
import com.example.zhttaskflow.base.ui.StateBox
import com.example.zhttaskflow.base.ui.TaskFlowScaffold
import com.example.zhttaskflow.base.ui.icon.TaskFlowIcons
import com.example.zhttaskflow.base.ui.rememberTaskFlowStateBoxContentPadding
import com.example.zhttaskflow.base.ui.extension.PageLifecycleLog
import com.example.zhttaskflow.base.ui.extension.logUiInteraction
import com.example.zhttaskflow.feature.article.R

/** 资讯详情页埋点 pageId（与全局 Analytics 约定一致）。 */
internal const val ARTICLE_DETAIL_PAGE_ID: String = "ArticleDetail"

/**
 * 文章详情页：WebView 加载 H5 链接；系统/顶栏返回经 [TaskFlowScaffold] 的 [onBackIntercept] 优先 WebView 历史栈。
 *
 * @param viewModel MVI 状态源
 * @param articleId 文章标识（展示用）
 * @param detailUrl 路由传入的详情链接（加载由 ViewModel 校验后写入 Success 态）
 * @param onNavigateUp 页面级返回（导航栈回退）
 */
@Composable
fun ArticleDetailScreen(
    viewModel: ArticleDetailViewModel,
    articleId: String,
    detailUrl: String,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectUiStateWithLifecycle()
    val emptyMessage = stringResource(id = R.string.article_str_detail_empty)
    val scaffoldTitle = stringResource(id = R.string.article_str_detail_title, articleId)
    val lifecycleArgs = when (val state = uiState) {
        is BaseUiState.Success -> "articleId=${state.data.articleId};url=${state.data.detailUrl}"
        is BaseUiState.Loading -> "loading"
        is BaseUiState.Error -> "error"
        is BaseUiState.Empty -> "empty"
    }

    LaunchedEffect(articleId, detailUrl) {
        viewModel.onEvent(ArticleDetailUiEvent.Load(articleId = articleId, detailUrl = detailUrl))
    }

    val webViewHolder = remember { ArticleDetailWebViewHolder() }
    val webViewBackIntercept: () -> Boolean = {
        val webView = webViewHolder.webView
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
            true
        } else {
            false
        }
    }

    PageLifecycleLog(
        pageName = ARTICLE_DETAIL_PAGE_ID,
        pageArgs = lifecycleArgs,
    )

    TaskFlowScaffold(
        modifier = modifier,
        title = scaffoldTitle,
        onNavigateUp = onNavigateUp,
        onBackIntercept = webViewBackIntercept,
        navigationIcon = {
            IconButton(
                onClick = {
                    logUiInteraction(
                        action = "click",
                        identifier = "article_detail_back",
                        pageId = ARTICLE_DETAIL_PAGE_ID,
                    )
                    handleTaskFlowPageBack(
                        onNavigateUp = onNavigateUp,
                        onBackIntercept = webViewBackIntercept,
                    )
                },
            ) {
                Icon(
                    imageVector = TaskFlowIcons.Nav.Back,
                    contentDescription = stringResource(id = R.string.article_str_back),
                )
            }
        },
    ) { _ ->
        StateBox(
            uiState = uiState,
            onRetry = {
                logUiInteraction(
                    action = "click",
                    identifier = "article_detail_network_retry",
                    pageId = ARTICLE_DETAIL_PAGE_ID,
                )
                viewModel.onEvent(ArticleDetailUiEvent.Retry)
            },
            emptyMessage = emptyMessage,
            contentPadding = rememberTaskFlowStateBoxContentPadding(),
            modifier = Modifier.fillMaxSize(),
        ) { data ->
            val currentDetailUrl = rememberUpdatedState(data.detailUrl)
            ArticleDetailWebView(
                detailUrl = data.detailUrl,
                webViewHolder = webViewHolder,
                currentDetailUrl = currentDetailUrl,
            )
        }
    }
}

@Composable
private fun ArticleDetailWebView(
    detailUrl: String,
    webViewHolder: ArticleDetailWebViewHolder,
    currentDetailUrl: State<String>,
) {
    // URL 变化时重建 AndroidView，onRelease 与视图实例生命周期对齐
    key(detailUrl) {
        // 压制 Compose Lint 对 AndroidView 的 Applier 上下文误报，运行时无任何问题
        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                createArticleWebView(context = context).also { created ->
                    webViewHolder.webView = created
                }
            },
            update = { webView ->
                val url = currentDetailUrl.value
                if (webView.url != url) {
                    webView.loadUrl(url)
                }
            },
            onRelease = { webView ->
                if (webViewHolder.webView === webView) {
                    webViewHolder.webView = null
                }
                releaseArticleWebView(webView)
            },
        )
    }
}

private class ArticleDetailWebViewHolder {
    var webView: WebView? = null
}

/**
 * 按官方顺序释放 WebView，避免泄漏与回调悬挂。
 */
private fun releaseArticleWebView(webView: WebView) {
    webView.stopLoading()
    webView.webViewClient = WebViewClient()
    webView.destroy()
}

/**
 * 初始化资讯详情 WebView（不在此处 loadUrl，由 [AndroidView] update 按 URL 统一加载）。
 */
@SuppressLint(
    "SetJavaScriptEnabled",
    "WrongThread",
    "InlinedApi",
)
private fun createArticleWebView(context: Context): WebView {
    return WebView(context).apply {
        // ViewCompat 在 API 26+ 生效，低版本自动忽略；NO 模式常量见 View#IMPORTANT_FOR_AUTOFILL_NO
        ViewCompat.setImportantForAutofill(this, View.IMPORTANT_FOR_AUTOFILL_NO)
        webViewClient = ArticleDetailWebViewClient()
        settings.apply {
            // 资讯 H5 依赖 JS 渲染，详情页必需启用；已收紧文件/内容访问与其它安全项
            javaScriptEnabled = true
            domStorageEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            allowContentAccess = false
            allowFileAccess = false
            @Suppress("DEPRECATION")
            savePassword = false
        }
    }
}

/**
 * 详情页 WebViewClient：预留加载失败、网络错误处理与埋点入口。
 */
private class ArticleDetailWebViewClient : WebViewClient() {
    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError,
    ) {
        super.onReceivedError(view, request, error)
        // 预留：加载失败日志 / 错误 UI，当前保持默认页面行为
    }
}
