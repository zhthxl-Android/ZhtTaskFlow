package com.example.zhttaskflow.nav.interceptor

import androidx.compose.runtime.compositionLocalOf

/**
 * 壳工程注入的登录会话；未提供时 [rememberTaskFlowLoginSession] 创建内存调试实例。
 */
val LocalTaskFlowLoginSession = compositionLocalOf<TaskFlowLoginSession?> { null }

/**
 * 壳工程注入的深链 URI → 内部 path 映射器；未提供时 [rememberTaskFlowDeepLinkRouteMapper] 使用默认样板规则。
 */
val LocalTaskFlowDeepLinkRouteMapper = compositionLocalOf<TaskFlowDeepLinkRouteMapper?> { null }
