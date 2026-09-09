package com.example.zhttaskflow.nav

import androidx.compose.runtime.compositionLocalOf

/**
 * 当前 NavHost 绑定的 [AppNavigator]，由 [AppNavHost] 注入，供业务 UI 消费导航副作用。
 */
val LocalNavigator = compositionLocalOf<AppNavigator> {
    error("AppNavigator 未提供，请在 AppNavHost 作用域内使用")
}
