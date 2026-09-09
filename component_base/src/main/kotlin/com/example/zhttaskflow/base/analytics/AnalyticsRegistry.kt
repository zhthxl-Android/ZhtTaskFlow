package com.example.zhttaskflow.base.analytics

import com.example.zhttaskflow.base.observability.DeveloperObservability

/**
 * 非 Composable 场景（如点击 lambda、[Modifier.clickWithLog]）解析当前 Analytics。
 */
internal object AnalyticsRegistry {

    private val stack = ArrayDeque<Analytics>()

    fun push(analytics: Analytics) {
        stack.addLast(analytics)
    }

    fun pop(analytics: Analytics) {
        if (stack.isNotEmpty() && stack.last() === analytics) {
            stack.removeLast()
        }
    }

    fun current(): Analytics {
        val shell = stack.lastOrNull() ?: AnalyticsFallback
        return DeveloperObservability.resolveAnalytics(shell)
    }
}
