package com.example.zhttaskflow.base.observability

/**
 * 可观测性统一发射框架
 * 第三方平台扩展接口，支持壳工程动态注入 APM / 埋点 / 崩溃 SDK 的实现，核心层无需感知具体第三方 SDK。
 * 壳工程注册的第三方埋点 / APM / 崩溃平台扩展口（默认空实现）。
 */
fun interface ObservabilityPlatformHook {

    /**
     * 当可观测数据经过核心管道处理完成后，会回调该方法，
     * 将数据同步上报给第三方平台（如 Bugly、友盟、神策等）。
     * 壳工程只需实现该接口并安装到管道，即可完成第三方 SDK 对接。
     * */
    fun onObservabilityEvent(
        channel: LocalObservabilityEmitter.Channel,
        payload: String,//管道格式化好的完整日志字符串，第三方可直接使用，也可自行解析 params
        pageId: String?,
        actionId: String?,
        params: Map<String, String?>?,
        throwable: Throwable?,
    )

    /**
     * 管道默认的钩子实现，空逻辑，什么都不做
     * 避免空指针，在未安装第三方平台时，管道依然可以正常运行，无需判空
     * */
    companion object {
        val NoOp: ObservabilityPlatformHook = ObservabilityPlatformHook { _, _, _, _, _, _ -> }
    }
}
