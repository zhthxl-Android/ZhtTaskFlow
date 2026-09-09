package com.example.zhttaskflow.nav.interceptor

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.zhttaskflow.nav.router.DeepLinkRouteInterceptor
import com.example.zhttaskflow.nav.router.RouteInterceptContext
import com.example.zhttaskflow.nav.router.RouteInterceptResult

/**
 * 深链导航标记：将外部 URI 包装为拦截链可识别的「虚拟路由」，由 [DeepLinkInterceptor] 解析并重定向为内部 path。
 *
 * 业务侧从 `Intent.data` 等入口调用 [wrap] 后交给 [com.example.zhttaskflow.nav.AppNavigator.navigate]。
 */
object RouteDeepLinkMarker {
    private const val ROUTE_PREFIX: String = "@deeplink/"

    /**
     * 构造带深链标记的导航 path（Navigation 不会直接注册该 path，仅拦截链消费）。
     */
    fun wrap(uri: String): String {
        val encoded = Uri.encode(uri.trim())
        return "$ROUTE_PREFIX$encoded"
    }

    /**
     * 从导航 path 中解析原始深链 URI；非深链路由返回 `null`。
     */
    fun parse(route: String): String? {
        if (!route.startsWith(ROUTE_PREFIX)) {
            return null
        }
        val encoded = route.removePrefix(ROUTE_PREFIX)
        if (encoded.isEmpty()) {
            return null
        }
        return Uri.decode(encoded)
    }
}

/**
 * 深链 URI → 应用内已注册路由 path 的映射器（样板可扩展）。
 */
fun interface DeepLinkRouteMapper {
    /**
     * @return 内部导航 path；无法识别时返回 `null` 并由拦截器提示用户
     */
    fun map(uri: Uri): String?
}

/**
 * 默认深链映射样板：约定 `taskflow://nav/route?target={urlEncodedPath}`。
 *
 * 扩展方式：在 [rememberDeepLinkRouteMapper] 的 `configure` 中追加自定义规则，或替换整个 [DeepLinkRouteMapper]。
 */
class DeepLinkRouteMapperImpl(
    private val rules: List<(Uri) -> String?>,
) : DeepLinkRouteMapper {

    override fun map(uri: Uri): String? {
        for (rule in rules) {
            val mapped = rule(uri)
            if (!mapped.isNullOrBlank()) {
                return mapped
            }
        }
        return null
    }

    class Builder {
        private val rules = mutableListOf<(Uri) -> String?>()

        /**
         * 注册一条映射规则；按注册顺序匹配，先命中先返回。
         */
        fun rule(handler: (Uri) -> String?) {
            rules.add(handler)
        }

        fun build(): DeepLinkRouteMapper {
            return DeepLinkRouteMapperImpl(rules.toList())
        }
    }

    companion object {
        private const val SAMPLE_SCHEME: String = "taskflow"
        private const val SAMPLE_HOST_NAV: String = "nav"
        private const val SAMPLE_PATH_ROUTE: String = "/route"
        private const val QUERY_TARGET: String = "target"

        fun defaultBuilder(): Builder {
            return Builder().apply {
                rule { uri ->
                    if (uri.scheme != SAMPLE_SCHEME || uri.host != SAMPLE_HOST_NAV) {
                        return@rule null
                    }
                    if (uri.path != SAMPLE_PATH_ROUTE) {
                        return@rule null
                    }
                    uri.getQueryParameter(QUERY_TARGET)?.let { target ->
                        Uri.decode(target).takeIf { it.isNotBlank() }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberDeepLinkRouteMapper(
    configure: (DeepLinkRouteMapperImpl.Builder.() -> Unit)? = null,
): DeepLinkRouteMapper {
    val injected = LocalDeepLinkRouteMapper.current
    if (injected != null) {
        return injected
    }
    return remember(configure) {
        val builder = DeepLinkRouteMapperImpl.defaultBuilder()
        configure?.invoke(builder)
        builder.build()
    }
}

/**
 * 深链拦截器：解析 [RouteDeepLinkMarker]、写入 [com.example.zhttaskflow.nav.router.RouteInterceptExtras.deepLinkUri]、
 * 重定向至业务路由；[com.example.zhttaskflow.nav.router.RouteInterceptResult.Redirect] 后拦截链 **继续** 执行（权限 / 登录等），
 * 与 App 内 `navigate` 一致。失败时 [RouteInterceptResult.Abort] 并由拦截链 UI 桥展示错误。
 */
class DeepLinkInterceptor(
    private val routeMapper: DeepLinkRouteMapper,
    private val parseErrorMessage: String,
    private val unmappedErrorMessage: String,
    override val priority: Int = RouterInterceptorPriorities.DEEP_LINK,
    override val showsLoading: Boolean = false,
) : DeepLinkRouteInterceptor {

    override suspend fun intercept(context: RouteInterceptContext): RouteInterceptResult {
        val rawUri = RouteDeepLinkMarker.parse(context.targetRoute)
            ?: return RouteInterceptResult.Proceed

        val uri = runCatching { Uri.parse(rawUri) }.getOrNull()
        if (uri == null) {
            return RouteInterceptResult.Abort(userMessage = parseErrorMessage)
        }

        context.extras.deepLinkUri = rawUri

        val mappedRoute = routeMapper.map(uri)
        if (mappedRoute.isNullOrBlank()) {
            return RouteInterceptResult.Abort(userMessage = unmappedErrorMessage)
        }

        return RouteInterceptResult.Redirect(mappedRoute)
    }
}
