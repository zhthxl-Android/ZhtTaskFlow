package com.example.zhttaskflow.nav

import com.example.zhttaskflow.nav.route.TaskFlowRoute
import com.example.zhttaskflow.nav.route.TaskFlowRouteArguments

/**
 * 对外统一跳转扩展，便于 Feature 通过持有的 [TaskFlowNavigator] 调用。
 */
fun TaskFlowNavigator.navigateTo(
    path: String,
    params: Map<String, String> = emptyMap(),
) {
    navigate(TaskFlowRoute(path), TaskFlowRouteArguments(params))
}

fun TaskFlowNavigator.popBack() {
    pop()
}
