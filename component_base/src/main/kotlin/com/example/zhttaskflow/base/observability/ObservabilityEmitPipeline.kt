package com.example.zhttaskflow.base.observability

import android.util.Log
import com.example.zhttaskflow.core.log.Logger
import com.example.zhttaskflow.core.observability.LocalLogStore

/**
 * 可观测性统一发射框架
 *  核心执行管道，负责「日志打印 → 本地落盘 → 第三方平台回调」三步串联处理，是整个链路的调度中枢
 * 可观测三联统一落盘；落盘后回调 [ObservabilityPlatformHook]。
 */
object ObservabilityEmitPipeline {

    //第三方平台钩子实例，默认空实现
    @Volatile
    var platformHook: ObservabilityPlatformHook = ObservabilityPlatformHook.NoOp

    /**
     * 对外暴露的安装方法，
     * 壳工程在 Application 启动时调用，注入自定义的第三方实现
     * */
    fun installPlatformHook(hook: ObservabilityPlatformHook) {
        platformHook = hook
    }

    /**
     * 整个可观测数据的处理入口，
     * 按顺序执行「格式化 → 日志打印 → 堆栈处理 → 本地落盘 → 平台回调」全链路逻辑
     * */
    fun emit(
        channel: LocalObservabilityEmitter.Channel,//数据通道类型，决定后续处理逻辑
        eventOrMetric: String,////事件名或指标名，如 `ACTION_PAGE_ENTER`、`METRIC_FIRST_FRAME`
        pageId: String?,
        actionId: String?,
        params: Map<String, String?>? = null,
        throwable: Throwable? = null,
    ) {
        //将所有参数拼接成一行结构化的日志字符串
        val payload = buildPayload(
            channel = channel,
            eventOrMetric = eventOrMetric,
            pageId = pageId,
            actionId = actionId,
            params = params,
        )
        //根据数据通道类型，决定日志打印的级别
        when (channel) {
            LocalObservabilityEmitter.Channel.CRASH -> Logger.errorAlways(
                LocalObservabilityEmitter.LOG_TAG,
                { payload },
                throwable
            )

            else -> Logger.i(
                LocalObservabilityEmitter.LOG_TAG,
                { payload }
            )
        }
        //将异常完整堆栈转为字符串
        val stackTrace = throwable?.let { stackTraceOf(it) }
        //将 `Channel` 枚举，转换为本地存储层 `LocalLogStore` 内部的通道枚举
        val storeChannel = when (channel) {
            LocalObservabilityEmitter.Channel.ANALYTICS -> LocalLogStore.ObservabilityChannel.ANALYTICS
            LocalObservabilityEmitter.Channel.PERFORMANCE -> LocalLogStore.ObservabilityChannel.PERFORMANCE
            LocalObservabilityEmitter.Channel.CRASH -> LocalLogStore.ObservabilityChannel.CRASH
        }
        //将数据持久化到本地文件
        LocalLogStore.recordFromObservabilityEmit(
            channel = storeChannel,
            eventOrMetric = eventOrMetric,
            pageId = pageId,
            actionId = actionId,
            params = params,
            stackTrace = stackTrace,
            //异常标记
            anomaly = params?.get("anomaly") == "true" || channel == LocalObservabilityEmitter.Channel.CRASH,
        )
        //如果第三方 SDK 内部发生崩溃，只会打印一条错误日志
        try {
            //调用第三方钩子的回调方法，将数据同步给外部平台
            platformHook.onObservabilityEvent(
                channel = channel,
                payload = payload,
                pageId = pageId,
                actionId = actionId,
                params = params,
                throwable = throwable,
            )
        } catch (t: Throwable) {
            Logger.errorAlways(
                LocalObservabilityEmitter.LOG_TAG,
                { "platform hook execution failed" },
                t,
            )
        }
    }

    /**
     * 构建结构化日志
     * 示例：channel=analytics event=page_enter pageId=MainActivity actionId=page_enter params=uid=1001,tab=home
     * */
    private fun buildPayload(
        channel: LocalObservabilityEmitter.Channel,
        eventOrMetric: String,
        pageId: String?,
        actionId: String?,
        params: Map<String, String?>?,
    ): String {
        return buildString {
            append("channel=${channel.name.lowercase()}")
            append(" event=$eventOrMetric")
            if (!pageId.isNullOrBlank()) {
                append(" pageId=$pageId")
            }
            if (!actionId.isNullOrBlank()) {
                append(" actionId=$actionId")
            }
            val snapshot = formatParams(params)
            if (snapshot.isNotBlank()) {
                append(" params=$snapshot")
            }
        }
    }

    /**
     * 将 Map 类型的参数，转为 `key=value,key2=value2` 格式的字符串。
     * */
    fun formatParams(params: Map<String, String?>?): String {
        return params
            ?.entries
            ?.mapNotNull { (key, value) -> value?.let { value -> "$key=$value" } }
            ?.joinToString(separator = ",")
            .orEmpty()
    }

    /**
     * 将 Throwable 异常对象转为完整的堆栈字符串
     * */
    private fun stackTraceOf(throwable: Throwable): String {
        return buildString {
            append(throwable::class.java.name)
            append(": ")
            append(throwable.message.orEmpty())
            append('\n')
            append(throwable.stackTraceToString())
        }
    }
}
