package com.example.zhttaskflow.feature.article.api

/**
 * 资讯业务专属玩 Android 开放 API 相对路径（单一事实来源）。
 *
 * BaseUrl 统一引用 [com.example.zhttaskflow.core.network.WanAndroidApiConfig.PRODUCTION_BASE_URL]；
 * 本对象仅维护接口 path，新增/修改路径仅需改动 feature_article 模块。
 */
object ArticleApiPaths {

    /** 玩 Android 文章列表：`article/list/{page}/json`，页码从 0 开始。
     * 该接口支持传入 page_size 控制分页数量，取值为[1-40]，不传则使用默认值，
     * 一旦传入了 page_size，后续该接口分页都需要带上，否则会造成分页读取错误。
     * 方法：GET
     * 参数：页码，拼接在连接中，从1开始。*/
    const val ARTICLE_LIST: String = "article/list/{page}/json"

    /** 玩 Android Banner 轮播。
     * 方法：GET
     * 参数：无 */
    const val BANNER: String = "banner/json"

    /** 搜索热词
    方法：GET
    参数：无*/
    const val KNOWLEDGE_TREE: String = "hotkey/json"
}
