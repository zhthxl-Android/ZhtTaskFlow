package com.example.zhttaskflow.nav

import androidx.compose.runtime.compositionLocalOf

/**
 * 当前 NavHost 绑定的 [TaskFlowNavigator]，由 [TaskFlowNavHost] 注入，供业务 UI 消费导航副作用。
 */
val LocalTaskFlowNavigator = compositionLocalOf<TaskFlowNavigator> {
    error("TaskFlowNavigator 未提供，请在 TaskFlowNavHost 作用域内使用")
}
