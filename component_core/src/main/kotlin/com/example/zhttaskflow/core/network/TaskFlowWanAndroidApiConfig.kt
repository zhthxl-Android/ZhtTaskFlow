package com.example.zhttaskflow.core.network

/**
 * 玩 Android 开放 API 网络基线配置（单一事实来源）。
 *
 * ## 官方权威信息
 * - 官方文档（项目备案短链）：[OFFICIAL_DOC_SHORT_URL]
 * - 正式环境 BaseUrl（项目备案短链）：[PRODUCTION_BASE_URL_SHORT_REF]
 * - 解析后正式域名：[PRODUCTION_BASE_URL]（与玩 Android 开放 API 一致）
 *
 * ## 多环境约定
 * - [PRODUCTION_BASE_URL]：线上正式环境，业务 Retrofit 默认应使用此地址
 * - 测试 / Mock 环境请在业务 DataConfig 或调试入口单独注入，禁止与正式常量混用
 *
 * ## 核心接口路径（均拼接在 BaseUrl 之后，供业务扩展参考）
 * | 能力 | 路径 | 说明 |
 * |------|------|------|
 * | 首页文章列表 | `article/list/{page}/json` | `page` 从 **0** 开始 |
 * | 首页 Banner | `banner/json` | 轮播图数据 |
 * | 知识体系分类 | `tree/json` | 分类树 |
 * | 公众号列表 | `wxarticle/chapters/json` | 公众号章节列表 |
 *
 * ## 使用方式
 * 业务模块通过 [RetrofitServiceFactory.createApi] 传入 [PRODUCTION_BASE_URL]，
 * 禁止在 Feature 内硬编码域名；路径常量见 [Paths]。
 *
 * ## 扩展预留
 * 后续可在此补充 Staging BaseUrl、路径常量统一出口及环境切换策略。
 */
object TaskFlowWanAndroidApiConfig {

    /** 玩 Android 官方文档（备案短链，指向开放 API 说明页）。 */
    const val OFFICIAL_DOC_SHORT_URL: String =
        "https://www.wanandroid.com/blog/show/2"

    /** 正式环境 BaseUrl 备案短链（解析目标为 [PRODUCTION_BASE_URL]）。
     * 优先使用wanandroid.com，不要带www，近期部分地区www的完整域名访问异常，暂未排查到原因*/
    const val PRODUCTION_BASE_URL_SHORT_REF: String =
        "https://wanandroid.com/"

    /**
     * 玩 Android **线上正式环境**全局 BaseUrl。
     *
     * Retrofit 要求以 `/` 结尾；与官方开放 API 域名 `www.wanandroid.com` 一致。
     */
    const val PRODUCTION_BASE_URL: String = "https://wanandroid.com/"

    /**
     * 相对 [PRODUCTION_BASE_URL] 的核心接口路径（玩 Android 开放 API 基线）。
     */
    object Paths {
        /** 首页文章列表：`article/list/{page}/json`，页码从 0 开始。
         * 该接口支持传入 page_size 控制分页数量，取值为[1-40]，不传则使用默认值，
         * 一旦传入了 page_size，后续该接口分页都需要带上，否则会造成分页读取错误。
         * 方法：GET
         * 参数：页码，拼接在连接中，从0开始。*/
        const val ARTICLE_LIST: String = "article/list/{page}/json"

        /** 首页 Banner 轮播。
         * 方法：GET
         * 参数：无 */
        const val BANNER: String = "banner/json"

        /** 搜索热词
        方法：GET
        参数：无*/
        const val KNOWLEDGE_TREE: String = "hotkey/json"
    }
}
