package com.example.zhttaskflow.nav.interceptor

import com.example.zhttaskflow.nav.route.TaskFlowArticleNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowHomeNavRoutes
import com.example.zhttaskflow.nav.route.TaskFlowTaskNavRoutes

/**
 * 业务路由门禁策略（登录 / 权限标记的单一事实来源）。
 *
 * RouteHost 在 `navigate` 前、深链 [com.example.zhttaskflow.nav.deeplink.TaskFlowDeepLinkNavigation.enrichExternalDeepLinkUri]
 * 对 `target` 处理时，均调用 [enrichNavigationPath]，保证内外跳转一致。
 *
 * 完整路由清单见工程文档 `docs/TASKFLOW_ROUTE_GATES.md`。
 */
object TaskFlowRouteGatePolicy {

    /**
     * 为需门禁的 **纯净** Navigation path 叠加 `needLogin` / `permissionGroup` query。
     *
     * 未在策略表中的 path 原样返回；已识别的详情 path 每次从 path 段重建标记，避免重复 query。
     */
    fun enrichNavigationPath(route: String): String {
        val pathOnly = route.substringBefore('?')
        return when {
            isTaskDetailNavigationPath(pathOnly) -> {
                TaskFlowRoutePermissionMarker.withStoragePermission(
                    TaskFlowRouteAuthMarker.withNeedLogin(pathOnly),
                )
            }
            isArticleDetailNavigationPath(pathOnly) -> {
                TaskFlowRouteAuthMarker.withNeedLogin(pathOnly)
            }
            else -> route
        }
    }

    /**
     * 任务详情：`feature_task/detail/{taskId}` → 登录 + 存储权限（附件/导出示范）。
     */
    fun isTaskDetailNavigationPath(pathWithoutQuery: String): Boolean {
        val expectedPrefix = "${TaskFlowTaskNavRoutes.TASK_DETAIL.substringBefore('{')}"
        if (!pathWithoutQuery.startsWith(expectedPrefix)) {
            return false
        }
        val taskId = pathWithoutQuery.removePrefix(expectedPrefix)
        return taskId.isNotBlank() && !taskId.contains('/')
    }

    /**
     * 资讯详情：`feature_article/detail/{articleId}/{detailUrl}` → 仅登录。
     */
    fun isArticleDetailNavigationPath(pathWithoutQuery: String): Boolean {
        val expectedPrefix = "${TaskFlowArticleNavRoutes.ARTICLE_DETAIL.substringBefore('{')}"
        if (!pathWithoutQuery.startsWith(expectedPrefix)) {
            return false
        }
        val remainder = pathWithoutQuery.removePrefix(expectedPrefix)
        val segments = remainder.split('/')
        return segments.size >= 2 &&
            segments[0].isNotBlank() &&
            segments[1].isNotBlank()
    }

    /** 首页 hub：无门禁（文档对照用）。 */
    fun isHomeNavigationPath(pathWithoutQuery: String): Boolean {
        return pathWithoutQuery == TaskFlowHomeNavRoutes.HOME_ROUTE
    }

    /** 任务列表：无门禁。 */
    fun isTaskListNavigationPath(pathWithoutQuery: String): Boolean {
        return pathWithoutQuery == TaskFlowTaskNavRoutes.TASK_LIST
    }

    /** 资讯列表：无门禁。 */
    fun isArticleListNavigationPath(pathWithoutQuery: String): Boolean {
        return pathWithoutQuery == TaskFlowArticleNavRoutes.ARTICLE_LIST
    }
}
