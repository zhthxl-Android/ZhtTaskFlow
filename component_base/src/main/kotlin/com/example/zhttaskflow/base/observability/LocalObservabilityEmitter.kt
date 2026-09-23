package com.example.zhttaskflow.base.observability

/**
 *
 * 可观测性统一发射框架
 * 对外统一门面，集中管理所有常量、通道枚举，提供业务方直接调用的发射入口，屏蔽内部实现细节。
 * 可观测契约常量与 Release 风格落盘（全工程唯一常量源）。
 *
 * Debug 面板切换 [com.example.zhttaskflow.core.debug.DeveloperTools.ObservabilityBackend.RELEASE_LOCAL] 时走 [emit]；
 * 生产壳层经 `app` 模块 [com.example.zhttaskflow.observability.ReleaseObservabilityContract.emit] 落盘并对接 SDK。
 */
object LocalObservabilityEmitter {

    //全局日志标签，固定值 `TaskFlow/Observability`，所有 logcat 输出统一使用该标签，方便日志过滤与排查。
    const val LOG_TAG: String = "TaskFlow/Observability"

    //埋点动作：页面进入
    const val ACTION_PAGE_ENTER: String = "page_enter"
    //埋点动作：页面离开
    const val ACTION_PAGE_LEAVE: String = "page_leave"
    //埋点动作：页面参数变更
    const val ACTION_PAGE_ARGS_CHANGE: String = "page_args_change"
    //埋点动作：全局未捕获崩溃
    const val ACTION_APP_UNCAUGHT_CRASH: String = "app_uncaught_crash"
    //埋点动作：应用 ANR（应用无响应）
    const val ACTION_APP_ANR: String = "app_anr"

    //性能指标：首帧渲染耗时
    const val METRIC_FIRST_FRAME: String = "first_frame"
    //性能指标：滑动帧率
    const val METRIC_SCROLL_FPS: String = "scroll_fps"
    //性能指标：页面停留时长
    const val METRIC_PAGE_DWELL: String = "page_dwell"

    //事件名：崩溃事件
    const val EVENT_CRASH: String = "crash"
    //事件名：ANR 事件
    const val EVENT_ANR: String = "anr"

    /**
     * 数据做类型划分，
     * 后续管道会根据通道类型执行差异化处理（如日志级别、落盘分类、异常标记）
     * */
    enum class Channel {
        ANALYTICS,//分析埋点通道：用户行为、页面事件等
        PERFORMANCE,// 性能监控通道：帧率、耗时、内存等指标
        CRASH,// 崩溃异常通道：崩溃、ANR 等异常事件
    }

    /**
     * 业务层调用的统一发射入口，内部直接透传所有参数给核心管道 `ObservabilityEmitPipeline.emit()`，
     * 本身不做业务逻辑，仅做门面隔离，降低业务层与核心实现的耦合。
     * */
    fun emit(
        channel: Channel,//数据通道类型，决定后续处理逻辑
        eventOrMetric: String,//事件名或指标名，如 `ACTION_PAGE_ENTER`、`METRIC_FIRST_FRAME`
        pageId: String?,//页面唯一标识，标记事件发生的页面
        actionId: String?,//动作唯一标识，标记具体的用户动作或系统动作
        params: Map<String, String?>? = null,//附加参数字典，支持自定义键值对
        throwable: Throwable? = null,//异常对象，仅崩溃 / ANR 类事件传入，用于打印堆栈与落盘
    ) {
        ObservabilityEmitPipeline.emit(
            channel = channel,
            eventOrMetric = eventOrMetric,
            pageId = pageId,
            actionId = actionId,
            params = params,
            throwable = throwable,
        )
    }
}
