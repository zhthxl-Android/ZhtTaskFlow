package com.example.zhttaskflow.feature.article.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import com.example.zhttaskflow.base.ui.TaskFlowScaffold
import com.example.zhttaskflow.feature.article.R

/**
 * 文章详情页：WebView 加载 H5 链接。
 *
 * @param articleId 文章标识（展示用）
 * @param detailUrl 详情链接
 * @param onNavigateUp 页面级返回（导航栈回退）
 */
@Composable
fun ArticleDetailScreen(
    articleId: String,
    detailUrl: String,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 仅供 BackHandler 读取当前 WebView，不在 update 中写入，避免与 AndroidView lambda 捕获规则冲突
    val webViewHolder = remember {
        object {
            var webView: WebView? = null
        }
    }
    val currentDetailUrl = rememberUpdatedState(detailUrl)
    val currentOnNavigateUp = rememberUpdatedState(onNavigateUp)

    BackHandler {
        val webView = webViewHolder.webView
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            currentOnNavigateUp.value()
        }
    }

    TaskFlowScaffold(
        modifier = modifier,
        title = stringResource(id = R.string.article_str_detail_title, articleId),
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.article_str_back),
                )
            }
        },
    ) { innerPadding ->
        // URL 变化时重建 AndroidView，onRelease 与视图实例生命周期对齐
        key(detailUrl) {
            // 压制 Compose Lint 对 AndroidView 的 Applier 上下文误报，运行时无任何问题
            @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
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
