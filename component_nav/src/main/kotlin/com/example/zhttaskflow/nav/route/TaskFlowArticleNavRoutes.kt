package com.example.zhttaskflow.nav.route

import android.net.Uri

/**
 * 资讯 Feature 全局路由常量（单一事实来源）。
 *
 * 业务 feature_article 仅引用本对象，不在模块内重复定义路由字符串。
 */
object TaskFlowArticleNavRoutes {
    const val LIST: String = "feature_article/list"
    const val DETAIL: String = "feature_article/detail/{articleId}/{detailUrl}"
    const val ARG_ARTICLE_ID: String = "articleId"
    const val ARG_DETAIL_URL: String = "detailUrl"

    /**
     * 生成详情页完整导航 path（用于 [com.example.zhttaskflow.nav.TaskFlowNavigator.navigate]）。
     */
    fun detailPath(articleId: String, detailUrl: String): String {
        val encodedId = Uri.encode(articleId)
        val encodedUrl = Uri.encode(detailUrl)
        return "feature_article/detail/$encodedId/$encodedUrl"
    }
}
