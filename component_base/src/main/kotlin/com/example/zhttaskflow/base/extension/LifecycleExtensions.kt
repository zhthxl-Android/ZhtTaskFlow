package com.example.zhttaskflow.base.extension

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 生命周期相关扩展，比如 Flow 安全收集、生命周期感知型协程启动
 */


/**
 * 在 [Lifecycle.State.STARTED] 及以上收集 Flow，避免后台无效重组/回调。
 */
fun <T> Flow<T>.collectIn(
    lifecycleOwner: LifecycleOwner,
    minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
    collector: suspend (T) -> Unit,
) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(minActiveState) {
            collect(collector)
        }
    }
}
