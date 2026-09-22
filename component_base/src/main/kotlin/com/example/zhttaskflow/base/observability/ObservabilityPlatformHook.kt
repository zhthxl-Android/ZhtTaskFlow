package com.example.zhttaskflow.base.observability

/**
 * 壳工程注册的第三方埋点 / APM / 崩溃平台扩展口（默认空实现）。
 */
fun interface ObservabilityPlatformHook {

    fun onObservabilityEvent(
        channel: LocalObservabilityEmitter.Channel,
        payload: String,
        pageId: String?,
        actionId: String?,
        params: Map<String, String?>?,
        throwable: Throwable?,
    )

    companion object {
        val NoOp: ObservabilityPlatformHook = ObservabilityPlatformHook { _, _, _, _, _, _ -> }
    }
}
