package com.example.zhttaskflow.nav.deeplink

import android.content.Intent
import android.net.Uri
import com.example.zhttaskflow.nav.interceptor.RouteDeepLinkMarker
import com.example.zhttaskflow.nav.interceptor.RouteGatePolicy

/**
 * 外部深链 URI → 拦截链导航 path（与集成壳 [com.example.zhttaskflow.MainActivity]、独立调试壳共用）。
 */
object DeepLinkNavigation {

    private const val SCHEME: String = "taskflow"
    private const val HOST_NAV: String = "nav"
    private const val PATH_ROUTE: String = "/route"
    private const val QUERY_TARGET: String = "target"

    /**
     * 从 `ACTION_VIEW` [Intent] 提取待处理深链；普通启动返回 `null`。
     */
    fun extractDeepLinkUri(intent: Intent?): String? {
        if (intent == null) {
            return null
        }
        if (intent.action != Intent.ACTION_VIEW) {
            return null
        }
        return intent.data?.toString()?.takeIf { it.isNotBlank() }
    }

    /**
     * 构造进入拦截链的导航 path：对 `target` 叠加壳层门禁后 [RouteDeepLinkMarker.wrap]。
     */
    fun prepareNavigationRoute(externalUri: String): String {
        val enrichedUri = enrichExternalDeepLinkUri(externalUri.trim())
        return RouteDeepLinkMarker.wrap(enrichedUri)
    }

    fun enrichExternalDeepLinkUri(externalUri: String): String {
        val uri = runCatching { Uri.parse(externalUri) }.getOrNull() ?: return externalUri
        val rawTarget = extractStandardDeepLinkTarget(uri) ?: return externalUri
        val gatedTarget = applyShellRouteGatePolicy(rawTarget)
        if (gatedTarget == rawTarget) {
            return externalUri
        }
        return buildStandardDeepLinkUri(gatedTarget)
    }

    private fun extractStandardDeepLinkTarget(uri: Uri): String? {
        if (uri.scheme != SCHEME) {
            return null
        }
        if (uri.host != HOST_NAV) {
            return null
        }
        if (uri.path != PATH_ROUTE) {
            return null
        }
        return uri.getQueryParameter(QUERY_TARGET)
            ?.let { target -> Uri.decode(target).takeIf { it.isNotBlank() } }
    }

    private fun buildStandardDeepLinkUri(encodedTargetPath: String): String {
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST_NAV)
            .appendPath(PATH_ROUTE.trimStart('/'))
            .appendQueryParameter(QUERY_TARGET, encodedTargetPath)
            .build()
            .toString()
    }

    /**
     * 与 Feature RouteHost 在 `navigate` 前的门禁标记对齐（新增页面在此扩展）。
     */
    fun applyShellRouteGatePolicy(mappedRoute: String): String {
        return RouteGatePolicy.enrichNavigationPath(mappedRoute)
    }
}
